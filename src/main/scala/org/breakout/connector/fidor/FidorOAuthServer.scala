package org.breakout.connector.fidor

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.http4s.HttpService
import org.http4s.dsl.{Root, _}
import org.http4s.server.blaze._

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

object FidorOAuthServer {

  private val log = Logger[FidorOAuthServer.type]
  private var apiCodeOption: Option[String] = None
  private val config = ConfigFactory.load()
  private val redirectUrl = config.getString("fidor.redirectUrl")
  private val redirectPort = config.getInt("fidor.redirectPort")

  private val service = HttpService {
    case req@GET -> Root =>

      apiCodeOption = req.uri.query.toMap.get("code").flatten

      apiCodeOption match {
        case Some(_) =>
          Ok().withBody("all ok, this can be closed")
        case None =>
          log.info("didn't get code")
          BadRequest().withBody("didn't get code")
      }
  }

  private def webServer(): BlazeBuilder = BlazeBuilder
    .bindHttp(redirectPort, redirectUrl)
    .mountService(service, "/")

  def fetchApiCode(): Future[String] = {
    val oAuthServer = webServer().run


    val system = ActorSystem("fidorCallbackAwait")
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
      oAuthServer.shutdownNow()
      result
    }
  }
}

