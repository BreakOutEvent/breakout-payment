package org.breakout.http

import java.awt.Desktop
import java.net.URI

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.breakout.UsageEnvironment._
import org.breakout.connector.fidor.{FidorApi, FidorOAuthServer, FidorTransaction}
import org.http4s._
import org.http4s.dsl.{->, GET, Ok, Root, _}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalatags.Text


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

  private val frontendService = HttpService {
    case GET -> Root => Ok(Html.htmlWrapper(Html.authorizePage(FidorOAuthServer.fidorOAuthRoute(clientId))).render)
      .putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))

    case GET -> Root / "transactions" => Ok(transactions.map(_.render))
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

    openBrowser(uri)

    BlazeBuilder
      .bindHttp(port, url)
      .mountService(frontendService, "/")
      .mountService(FidorOAuthServer.oauthService(WEB_FRONTEND), FidorOAuthServer.redirectRoute)
      .run
  }
}
