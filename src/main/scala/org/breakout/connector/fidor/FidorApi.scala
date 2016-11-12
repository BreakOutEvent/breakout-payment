package org.breakout.connector.fidor

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.breakout.connector.HttpConnection._
import org.breakout.connector.fidor.FidorRoutes._
import spray.client.pipelining._
import spray.http.{MediaTypes, OAuth2BearerToken}
import spray.httpx.SprayJsonSupport._
import spray.json.{DefaultJsonProtocol, NullOptions}

import scala.concurrent.Future


object FidorJsonProtocol extends DefaultJsonProtocol with NullOptions {
  implicit val fidorUserFormat = jsonFormat5(FidorUser)
  implicit val fidorTransactionTypeDetailsFormat = jsonFormat9(FidorTransactionTypeDetails)
  implicit val fidorTransactionFormat = jsonFormat13(FidorTransaction)
  implicit val fidorCollectionFormat = jsonFormat5(FidorCollection)
  implicit val fidorTransactionsFormat = jsonFormat2(FidorTransactions)
}

case class FidorRoute(url: String)

object FidorRoutes {

  private val config = ConfigFactory.load()
  private val baseUrl = config.getString("fidor.url")

  val USERS_CURRENT = FidorRoute(s"$baseUrl/users/current")
  val TRANSACTIONS = FidorRoute(s"$baseUrl/transactions")
}


object FidorApi {

  private val config = ConfigFactory.load()
  private val fidorToken = config.getString("fidor.token")

  private implicit val system = ActorSystem()

  import FidorJsonProtocol._
  import system.dispatcher

  private val fidorPipeline = (
    addHeader("Accept", "application/vnd.fidor.de; version=1,text/json")
      ~> addCredentials(OAuth2BearerToken(fidorToken))
      // ~> logReq
      ~> sendReceive
      //  ~> logResp
      ~> setContentType(MediaTypes.`application/json`)
    )

  def getUser: Future[FidorUser] = {
    val pipeline = fidorPipeline ~> unmarshal[FidorUser]
    pipeline(Get(USERS_CURRENT.url))
  }

  // TODO implement pagination or limit by date
  def getTransactions: Future[FidorTransactions] = {
    val pipeline = fidorPipeline ~> unmarshal[FidorTransactions]
    pipeline(Get(TRANSACTIONS.url))
  }
}
