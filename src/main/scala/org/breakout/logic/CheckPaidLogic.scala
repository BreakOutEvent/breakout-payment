package org.breakout.logic

import com.typesafe.scalalogging.Logger
import org.breakout.CmdConfig
import org.breakout.connector.fidor.FidorApi
import org.breakout.util.IntUtils._
import org.breakout.util.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object CheckPaidLogic {

  private val log = Logger[CheckPaidLogic.type]

  def doPaidCheck(cmdConfig: CmdConfig) = {
    log.info(s"Start doPaidCheck ${cmdConfig.dryRun.name}")

    FidorApi.getAllTransactions onComplete {
      case Success(transactions) =>
        val withCorrectSubject = transactions.filter(_.subject.hasValidSubject)

        log.debug(s"Got ${withCorrectSubject.size} transactions, with correct subject")

        withCorrectSubject.foreach { transaction =>
          log.info(s"received ${transaction.amount.toDecimalAmount}€ with ${transaction.subject.getSubjectCode} as id ${transaction.id} ")
        }

        System.exit(1)

      case Failure(e) => e.printStackTrace()
    }
  }


}
