package org.breakout.connector.backend

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.breakout.connector.HttpConnection._
import org.breakout.connector.backend.BackendRoutes.ADD_PAYMENT
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json.{DefaultJsonProtocol, NullOptions}

import scala.concurrent.Future

object BackendJsonProtocol extends DefaultJsonProtocol with NullOptions {
  implicit val backendPaymentFormat = jsonFormat3(BackendPayment)
  implicit val backendInvoiceFormat = jsonFormat2(BackendInvoice)
}

case class BackendRoute(url: String)

object BackendRoutes {

  private val config = ConfigFactory.load()
  private val baseUrl = config.getString("backend.url")

  def ADD_PAYMENT(purposeOfTransferCode: String): BackendRoute =
    BackendRoute(s"$baseUrl/invoice/payment/$purposeOfTransferCode/")
}

object BackendApi {

  private val log = Logger[BackendApi.type]
  private val config = ConfigFactory.load()
  private val authToken = config.getString("backend.authToken")
  private implicit val system = ActorSystem()

  import BackendJsonProtocol._
  import system.dispatcher

  private val backendPipeline = (
    addHeader("X-AUTH-TOKEN", authToken)
      // ~> logReq
      ~> sendReceive
      // ~> logResp
      ~> setContentType(MediaTypes.`application/json`)
    )

  def addPayment(purposeOfTransferCode: String, payment: BackendPayment): Future[BackendInvoice] = {
    log.debug(s"adding Payment $purposeOfTransferCode; $payment")
    val pipeline = backendPipeline ~> unmarshal[BackendInvoice]
    pipeline(Post(ADD_PAYMENT(purposeOfTransferCode).url, payment))
  }

}
