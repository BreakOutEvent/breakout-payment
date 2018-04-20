package org.breakout.connector.fidor

import java.net.URLEncoder
import java.util.UUID

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.breakout.UsageEnvironment
import org.breakout.UsageEnvironment._
import org.http4s.{Header, HttpService}
import org.http4s.dsl.{Root, _}
import org.http4s.server.Server
import org.http4s.server.blaze._

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

object FidorOAuthServer {

  private val log = Logger[FidorOAuthServer.type]
  private var apiCodeOption: Option[String] = None

  private val config = ConfigFactory.load()
  private val fidorApmUrl = config.getString("fidor.apmUrl")
  private val redirectUrl = config.getString("fidor.redirectUrl")
  private val redirectPort = config.getInt("fidor.redirectPort")

  val redirectRoute = "/auth/"
  val redirectUri = s"http://$redirectUrl:$redirectPort$redirectRoute"

  def fidorOAuthRoute(clientId: String): String = {
    val state = UUID.randomUUID().toString
    val urlEncoded = URLEncoder.encode(FidorOAuthServer.redirectUri, "UTF-8")

    s"$fidorApmUrl/oauth/authorize?client_id=$clientId&response_type=code&redirect_uri=$urlEncoded&state=$state"
  }

  def oauthService(usageEnvironment: UsageEnvironment) = HttpService {
    case req@GET -> Root =>

      apiCodeOption = req.uri.query.toMap.get("code").flatten

      apiCodeOption match {
        case Some(_) =>
          usageEnvironment match {
            case WEB_FRONTEND =>
              Ok("<script>window.location.href = '/transactions';</script>")
                .putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))
            case CLI =>
              Ok("<script>window.close();</script>")
                .putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))
          }
        case None =>
          log.info("didn't get code")
          BadRequest().withBody("didn't get code")
      }
  }

  def fetchApiCode(usageEnvironment: UsageEnvironment): Future[String] = {
    val oAuthServer: Option[Server] = usageEnvironment match {
      case WEB_FRONTEND => None
      case CLI => Some(BlazeBuilder.bindHttp(redirectPort, redirectUrl).mountService(oauthService(usageEnvironment), redirectRoute).run)
    }

    val system = ActorSystem("fidor-callback-await")
    import system.dispatcher

    val promise = Promise[String]

    log.debug("waiting for fidor callback with api code")
    val cancellable = system.scheduler.schedule(0.seconds, 2.seconds) {
      apiCodeOption match {
        case Some(apiCode) =>
          log.debug(s"got api code from fidor: $apiCode")
          promise.success(apiCode)
        case None =>
          log.trace("waiting for fidor api code")
      }
    }

    promise.future.map { result =>
      cancellable.cancel()
      system.terminate()
      oAuthServer.foreach(_.shutdownNow())
      result
    }
  }
}

