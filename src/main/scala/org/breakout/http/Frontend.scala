package org.breakout.http

import java.awt.Desktop
import java.net.URI

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.http4s.dsl.{->, GET, Ok, Root, _}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.{Header, HttpService}

import scalatags.Text
import scalatags.Text.all._

object Frontend {

  private val log = Logger[Frontend.type]
  private val config = ConfigFactory.load()
  private val url = config.getString("fidor.redirectUrl")
  private val port = config.getInt("fidor.redirectPort")


  val htmlWrapper: Text.TypedTag[String] = html(
    head(title := "BreakOut Fidor Payment"),
    body(
      div(
        h1(id := "title", "BreakOut Fidor Payment"),
        a(href := "authorize", "Fidor Zugriff authorisieren")
      )
    )
  )

  private val service = HttpService {
    case GET -> Root => Ok(htmlWrapper.render).putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))
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

    BlazeBuilder.bindHttp(port, url).mountService(service, "/").run
  }
}
