package org.breakout.logic

import com.typesafe.scalalogging.Logger
import org.breakout.CmdConfig
import org.breakout.connector.backend.{BackendApi, BackendPayment}
import org.breakout.connector.fidor.FidorApi
import org.breakout.util.IntUtils._
import org.breakout.util.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


object CheckPaidLogic {

  private val log = Logger[CheckPaidLogic.type]

  def doPaidCheck(cmdConfig: CmdConfig) = {
    log.info(s"Start doPaidCheck ${cmdConfig.dryRun.name}")

    FidorApi.getAllTransactions onComplete {
      case Success(transactions) =>
        val withCorrectSubject = transactions.filter(_.subject.hasValidSubject)

        log.debug(s"Got ${withCorrectSubject.size} transactions, with correct subject")

        Future.sequence(withCorrectSubject.map { transaction =>
          log.info(s"received ${transaction.amount.toDecimalAmount}€ with ${transaction.subject.getSubjectCode} as id ${transaction.id} ")

          if (!cmdConfig.dryRun.enabled) {
            BackendApi.addPayment(
              transaction.subject.getSubjectCode,
              BackendPayment(transaction.amount.toDecimalAmount, transaction.id.toLong)
            ) map { invoice =>
              log.info(s"SUCCESS: inserted payment to backend invoice $invoice")
            }
          } else {
            log.info("Won't insert payments to backend due to dry-running")
            Future.successful()
          }
        }) onComplete { _ =>
          System.exit(1)
        }

      case Failure(e) => e.printStackTrace()
    }
  }


}
