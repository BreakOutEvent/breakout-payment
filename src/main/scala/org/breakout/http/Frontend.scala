package org.breakout.http

import java.awt.Desktop
import java.net.URI

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.breakout.UsageEnvironment._
import org.breakout.connector.backend.{BackendApi, BackendPayment}
import org.breakout.connector.fidor.{FidorApi, FidorOAuthServer, FidorTransaction}
import org.breakout.http.html.Html
import org.breakout.logic.CheckPaidLogic.log
import org.http4s._
import org.http4s.dsl.{->, GET, Ok, Root, _}
import org.http4s.headers.Location
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalatags.Text
import org.breakout.util.IntUtils._
import org.breakout.util.StringUtils._


object Frontend {

  private val log = Logger[Frontend.type]
  private val config = ConfigFactory.load()
  private val url = config.getString("fidor.redirectUrl")
  private val port = config.getInt("fidor.redirectPort")
  private val clientId = config.getString("fidor.clientId")
  val fidorApi = new FidorApi(WEB_FRONTEND)

  def transactions: Future[Text.TypedTag[String]] =
    fidorApi.getAllTransactions.map { transactions: Seq[FidorTransaction] =>
      Html.htmlWrapper(Html.transactionsPage(transactions))
    }

  def transfer: Future[Text.TypedTag[String]] =
    fidorApi.getAllTransactions.flatMap { transactions: Seq[FidorTransaction] =>
      val withCorrectSubject = transactions.filter(_.subject.hasValidSubject)

      Future.sequence(withCorrectSubject.map { transaction =>
        BackendApi.addPayment(
          transaction.subject.getSubjectCode,
          BackendPayment(transaction.amount.toDecimalAmount, transaction.id.toLong)
        ) map { invoice =>
          log.info(s"SUCCESS: inserted payment to backend invoice $invoice")
        } recover { case e: Throwable =>
          log.error(s"backend rejected, maybe already inserted: ${transaction.amount.toDecimalAmount}€ as ${transaction.subject} from ${transaction.transaction_type_details.remote_name} (${transaction.transaction_type_details.remote_iban}) on ${transaction.value_date.getOrElse("")}; fidor id: ${transaction.id}")
        }
      }) map { _ =>
        Html.htmlWrapper(Html.transactionsPage(withCorrectSubject))
      }
    }

  private val frontendService = HttpService {
    case GET -> Root => Ok(Html.htmlWrapper(Html.authorizePage(FidorOAuthServer.fidorOAuthRoute(clientId))).render)
      .putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))

    case GET -> Root / "transactions" => Ok(transactions.map(_.render))
      .putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))

    case GET -> Root / "transfer" => Ok(transfer.map(_.render))
      .putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))

  }

  private def openBrowser(uri: String) =
    if (Desktop.isDesktopSupported) {
      Desktop.getDesktop.browse(new URI(uri))
    } else {
      Runtime.getRuntime.exec(s"xdg-open $uri")
    }

  def runWebServer(): Server = {
    val uri = s"http://$url:$port/"
    log.info(s"Frontend running: $uri")

    //openBrowser(uri)

    BlazeBuilder
      .bindHttp(port, url)
      .mountService(frontendService, "/")
      .mountService(FidorOAuthServer.oauthService(WEB_FRONTEND), FidorOAuthServer.redirectRoute)
      .run
  }
}
