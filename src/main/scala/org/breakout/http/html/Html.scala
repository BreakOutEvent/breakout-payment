package org.breakout.http.html

import org.breakout.connector.backend.{BackendInvoice, BackendPayment}
import org.breakout.connector.fidor.FidorTransaction
import org.breakout.util.StringUtils._
import org.breakout.util.IntUtils._
import scalatags.Text
import scalatags.Text.all.{span, _}

object Html {


  def htmlWrapper(content: Text.TypedTag[String]): Text.TypedTag[String] =
    html(
      head(
        title := "BreakOut Fidor Payment",
        tag("style")(`type` := "text/css", Style.styleSheetText)
      ),
      body(
        div(
          h1("BreakOut Fidor Payment"),
          content
        )
      )
    )

  def authorizePage(fidorLink: String): Text.TypedTag[String] =
    a(href := fidorLink, "authorize Fidor access")

  def transactionsPage(transactions: Seq[FidorTransaction], backendPayments: Seq[BackendPayment]): Text.TypedTag[String] = {

    def transactionClass(transaction: FidorTransaction, matchedFromBackend: Option[BackendPayment]) =
      (transaction.subject.hasValidSubject, matchedFromBackend) match {
        case (true, None) => Style.validNew
        case (true, Some(_)) => Style.valid
        case (false, _) => Style.invalid
      }

    // duplicate code, thanks quincy, i am to lazy
    def transactionIdentifierEmoji(transaction: FidorTransaction, matchedFromBackend: Option[BackendPayment]) =
      (transaction.subject.hasValidSubject, matchedFromBackend) match {
        case (true, None) => "\uD83D\uDE10"
        case (true, Some(_)) => "\uD83D\uDE0A"
        case (false, _) => "\uD83D\uDE1F"
      }

    div(
      h2("meaning:"),
      div(cls := Style.valid.name, "\uD83D\uDE0A already teansfered"),
      div(cls := Style.validNew.name, "\uD83D\uDE10 code valid, not transferred"),
      div(cls := Style.invalid.name, "\uD83D\uDE1F code invalid"),
      h2("transactions:"),
      a(cls := Style.backend.name, href := "/transfer", "transfer valid to backend"),
      ul(
        transactions.map { transaction =>
          val matchedFromBackend: Option[BackendPayment] = backendPayments.find(_.fidorId.contains(transaction.id.toLong))

          li(
            cls := transactionClass(transaction, matchedFromBackend).name,
            span(transactionIdentifierEmoji(transaction, matchedFromBackend)),
            span(cls := Style.date.name, transaction.value_date.getOrElse("").toString),
            span(cls := Style.subject.name, transaction.subject),
            span(cls := Style.date.name, transaction.amount.toEuroString),
            span(cls := Style.fidorId.name, s"(${transaction.id})")
          )
        }
      )
    )
  }

  def transferredPage(transferSummary: Seq[Either[(FidorTransaction, Throwable), (FidorTransaction, BackendInvoice)]]): Text.TypedTag[String] = {
    val transferred = transferSummary collect { case Right(x) => x }
    val errored = transferSummary collect { case Left(x) => x }

    div(
      h2("transferred:"),
      ul(
        transferred.map { case (transaction, invoice) =>
          li(
            span(cls := Style.date.name, transaction.value_date.getOrElse("").toString),
            span(cls := Style.subject.name, transaction.subject),
            span(cls := Style.fidorId.name, s"(${transaction.id})"),
            span(cls := Style.date.name, transaction.amount.toEuroString),
            span(cls := Style.fidorId.name, s"invoice #${invoice.id}")
          )
        }
      ),
      h2("errored:"),
      ul(
        errored.map { case (transaction, error) =>
          li(
            span(cls := Style.date.name, transaction.value_date.getOrElse("").toString),
            span(cls := Style.subject.name, transaction.subject),
            span(cls := Style.fidorId.name, error.getMessage)
          )
        }
      )
    )
  }


}
