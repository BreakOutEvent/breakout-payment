package org.breakout.logic

import com.typesafe.scalalogging.Logger
import org.breakout.CmdConfig
import org.breakout.connector.fidor.FidorApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object CheckPaidLogic {

  val log = Logger[CheckPaidLogic.type]

  def doPaidCheck(cmdConfig: CmdConfig) = {
    log.info(s"Start doPaidCheck ${cmdConfig.dryRun.name}")

    FidorApi.getTransactions onComplete {
      case Success(transactions) =>
        log.debug(s"Got ${transactions.collection} page")
        log.debug(s"Got ${transactions.data.size} transactions")
        System.exit(1)
      case Failure(e) => e.printStackTrace()
    }
  }
}
