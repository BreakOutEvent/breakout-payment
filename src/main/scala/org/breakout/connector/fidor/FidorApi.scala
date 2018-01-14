package org.breakout.connector.fidor

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.breakout.UsageEnvironment
import org.breakout.connector.HttpConnection._
import org.breakout.connector.fidor.FidorRoutes._
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json.{DefaultJsonProtocol, NullOptions}

import scala.concurrent.Future


object FidorJsonProtocol extends DefaultJsonProtocol with NullOptions {
  implicit val fidorUserFormat = jsonFormat5(FidorUser)
  implicit val fidorTransactionTypeDetailsFormat = jsonFormat9(FidorTransactionTypeDetails)
  implicit val fidorTransactionFormat = jsonFormat13(FidorTransaction)
  implicit val fidorCollectionFormat = jsonFormat5(FidorCollection)
  implicit val fidorTransactionsFormat = jsonFormat2(FidorTransactions)
  implicit val fidorAuthTokensFormat = jsonFormat4(FidorAuthTokens)
}

case class FidorRoute(url: String)

object FidorRoutes {

  private val config = ConfigFactory.load()
  private val fidorApiUrl = config.getString("fidor.apiUrl")
  private val fidorApmUrl = config.getString("fidor.apmUrl")

  val USERS_CURRENT = FidorRoute(s"$fidorApiUrl/users/current")

  def TRANSACTIONS(page: Int) = FidorRoute(s"$fidorApiUrl/transactions?per_page=100&page=$page")

  def AUTHORIZE(clientId: String): FidorRoute = FidorRoute(FidorOAuthServer.fidorOAuthRoute(clientId))

  val TOKEN: FidorRoute = FidorRoute(s"$fidorApmUrl/oauth/token")

}


class FidorApi(usageEnvironment: UsageEnvironment) {

  private val log = Logger[FidorApi]
  private val config = ConfigFactory.load()
  private var fidorTokenOption: Option[String] = None
  private val clientId = config.getString("fidor.clientId")
  private val clientSecret = config.getString("fidor.clientSecret")

  private implicit val system = ActorSystem()

  import FidorJsonProtocol._
  import system.dispatcher

  private lazy val basicFidorPipeline = (
    addHeader("Accept", "application/vnd.fidor.de; version=1,text/json")
      ~> addCredentials(BasicHttpCredentials(clientId, clientSecret))
      //~> logReq
      ~> sendReceive
      // ~> logResp
      ~> setContentType(MediaTypes.`application/json`)
    )

  private def fidorPipeline(fidorToken: String) = (
    addHeader("Accept", "application/vnd.fidor.de; version=1,text/json")
      ~> addCredentials(OAuth2BearerToken(fidorToken))
      // ~> logReq
      ~> sendReceive
      //~> logResp
      ~> fixLocationHeader
      ~> setContentType(MediaTypes.`application/json`)
    )

  def getUser: Future[FidorUser] = {
    getAuthorizedPipeline() flatMap { fidorToken =>
      log.debug("getting fidor user")
      val pipeline = fidorPipeline(fidorToken) ~> unmarshal[FidorUser]
      pipeline(Get(USERS_CURRENT.url))
    }
  }

  def getTransactions(page: Int): Future[FidorTransactions] = {
    getAuthorizedPipeline() flatMap { fidorToken =>
      val pipeline = fidorPipeline(fidorToken) ~> unmarshal[FidorTransactions]
      log.debug(s"getting fidor transactions page $page")
      pipeline(Get(TRANSACTIONS(page).url))
    }
  }

  def getAllTransactions: Future[Seq[FidorTransaction]] = {
    log.debug("getting all fidor transactions")
    getTransactions(1) flatMap { transactions =>
      val allPages = transactions.collection.total_pages

      Future.sequence((2 to allPages).map { page =>
        getTransactions(page)
      }) flatMap { transactionsList =>
        Future.successful(transactions.data ++ transactionsList.flatten(_.data))
      }
    }
  }

  private def getAuthorizedPipeline() = {
    fidorTokenOption match {
      case Some(fidorToken) => Future.successful(fidorToken)
      case None => authorizeFidor().map { fidorAuthTokens =>
        fidorTokenOption = Some(fidorAuthTokens.access_token)
        fidorAuthTokens.access_token
      }
    }
  }

  private def authorizeFidor(): Future[FidorAuthTokens] = {
    log.info(s"authorize with fidor: ${AUTHORIZE(clientId).url}")
    FidorOAuthServer.fetchApiCode(usageEnvironment) flatMap { apiCode =>
      val pipeline = basicFidorPipeline ~> unmarshal[FidorAuthTokens]

      pipeline(Post(TOKEN.url, FormData(Seq(
        "grant_type" -> "authorization_code",
        "code" -> apiCode,
        "redirect_uri" -> FidorOAuthServer.redirectUri,
        "client_id" -> clientId))
      ))
    }
  }
}
