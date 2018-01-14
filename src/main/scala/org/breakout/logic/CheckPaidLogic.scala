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

  def doPaidCheck(cmdConfig: CmdConfig) {
    log.info(s"Start doPaidCheck ${cmdConfig.dryRun.name}")
    val fidorApi = new FidorApi(cmdConfig.usageEnvironment)

    fidorApi.getAllTransactions onComplete {
      case Success(transactions) =>
        val withCorrectSubject = transactions.filter(_.subject.hasValidSubject)
        val withoutCorrectSubject = transactions.filter(t => !withCorrectSubject.contains(t))

        log.debug(s"Got ${withCorrectSubject.size} transactions, with correct subject")

        Future.sequence(withCorrectSubject.map { transaction =>
          log.debug(s"received ${transaction.amount.toDecimalAmount}€ with ${transaction.subject.getSubjectCode} as id ${transaction.id} ")

          if (!cmdConfig.dryRun.enabled) {
            BackendApi.addPayment(
              transaction.subject.getSubjectCode,
              BackendPayment(transaction.amount.toDecimalAmount, transaction.id.toLong)
            ) map { invoice =>
              log.info(s"SUCCESS: inserted payment to backend invoice $invoice")
            } recover { case _: Throwable =>
              log.error(s"backend rejected, maybe already inserted: ${transaction.amount.toDecimalAmount}€ as ${transaction.subject} from ${transaction.transaction_type_details.remote_name} (${transaction.transaction_type_details.remote_iban}) on ${transaction.value_date.getOrElse("")}; fidor id: ${transaction.id}")
            }
          } else {
            log.info("Won't insert payments to backend due to dry-running")
            Future.successful()
          }
        }) onComplete { _ =>
          withoutCorrectSubject.foreach { t =>
            log.error(s"subject is not matching for: ${t.amount.toDecimalAmount}€ as ${t.subject} from ${t.transaction_type_details.remote_name.getOrElse("")} (${t.transaction_type_details.remote_iban.getOrElse("")}) on ${t.value_date.getOrElse("")}; fidor id: ${t.id}")
          }
          System.exit(1)
        }

      case Failure(e) => e.printStackTrace()
    }
  }


}
