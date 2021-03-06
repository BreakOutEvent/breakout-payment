package org.breakout.http

import java.awt.Desktop
import java.net.URI

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.breakout.UsageEnvironment._
import org.breakout.connector.backend.{BackendApi, BackendInvoice, BackendPayment}
import org.breakout.connector.fidor.{FidorApi, FidorOAuthServer, FidorTransaction}
import org.breakout.http.html.Html
import org.breakout.util.IntUtils._
import org.breakout.util.StringUtils._
import org.http4s._
import org.http4s.dsl.{->, GET, Ok, Root, _}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import scalatags.Text

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

object Frontend {

  private val log = Logger[Frontend.type]
  private val config = ConfigFactory.load()
  private val url = config.getString("fidor.redirectUrl")
  private val port = config.getInt("fidor.redirectPort")
  private val clientId = config.getString("fidor.clientId")
  val fidorApi = new FidorApi(WEB_FRONTEND)

  def transactions: Future[Text.TypedTag[String]] =
    BackendApi.getAllPayments.flatMap { backendPayments: Seq[BackendPayment] =>
      fidorApi.getAllTransactions.map { transactions: Seq[FidorTransaction] =>
        Html.htmlWrapper(Html.transactionsPage(transactions, backendPayments))
      }
    }

  def transfer: Future[Text.TypedTag[String]] =
    BackendApi.getAllPayments.flatMap { backendPayments: Seq[BackendPayment] =>
      fidorApi.getAllTransactions.flatMap { transactions: Seq[FidorTransaction] =>
        val withCorrectSubject = transactions.filter(_.subject.hasValidSubject)
        val withoutAlreadyTransferred = withCorrectSubject.filterNot { transaction =>
          backendPayments.exists(_.fidorId.contains(transaction.id.toLong))
        }

        Future.sequence(withoutAlreadyTransferred.map { transaction =>
          BackendApi.addPayment(
            purposeOfTransferCode = transaction.subject.getSubjectCode,
            payment = BackendPayment(
              amount = transaction.amount.toDecimalAmount,
              fidorId = Some(transaction.id.toLong),
              date = transaction.value_date.flatMap(_.toUtcLong)
            )
          ) map { invoice =>
            log.info(s"SUCCESS: inserted payment to backend invoice $invoice")
            Right(transaction, invoice)
          } recover { case e: Throwable =>
            log.error(s"backend rejected fidor id: ${transaction.id} with ${e.getMessage}")
            Left(transaction, e)
          }
        }) map { transferSummary =>
          Html.htmlWrapper(Html.transferredPage(transferSummary))
        }
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

  private def openBrowser(uri: String) = {

    val os: String = System.getProperty("os.name").toLowerCase
    val isWindows = os.contains("win")
    val isLinux = os.contains("nux") || os.contains("nix")
    val isMac = os.contains("mac")

    if (Desktop.isDesktopSupported) {
      Desktop.getDesktop.browse(new URI(uri))
    } else if (isMac) {
      Runtime.getRuntime.exec(s"open $uri")
    } else if (isLinux) {
      Runtime.getRuntime.exec(s"xdg-open $uri")
    } else if (isWindows) {
      Runtime.getRuntime.exec(s"rundll32 url.dll,FileProtocolHandler $uri")
    }
  }

  def runWebServer(): Server = {
    val uri = s"http://$url:$port/"
    log.info(s"Frontend running: $uri")

    //openBrowser(uri)

    BlazeBuilder
      .withIdleTimeout(Duration.Inf)
      .bindHttp(port, url)
      .mountService(frontendService, "/")
      .mountService(FidorOAuthServer.oauthService(WEB_FRONTEND), FidorOAuthServer.redirectRoute)
      .run
  }
}
