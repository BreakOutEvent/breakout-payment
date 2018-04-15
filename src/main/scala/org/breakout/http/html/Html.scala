package org.breakout.http.html

import org.breakout.connector.backend.BackendPayment
import org.breakout.connector.fidor.FidorTransaction
import org.breakout.util.StringUtils._
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

    div(
      h2("meaning:"),
      div(cls := Style.valid.name, "already teansfered"),
      div(cls := Style.validNew.name, "code valid, not transfered"),
      div(cls := Style.invalidUnknown.name, "code unknown"),
      div(cls := Style.invalid.name, "code invalid"),
      h2("transactions:"),
      a(cls := Style.backend.name, href := "/transfer", "transfer valid to backend"),
      ul(
        transactions.map { transaction =>
          val matchedFromBackend: Option[BackendPayment] = backendPayments.find(_.fidorId == transaction.id.toLong)

          li(
            cls := transactionClass(transaction, matchedFromBackend).name,
          span(cls := Style.date.name, transaction.value_date.getOrElse("").toString),
          span(cls := Style.subject.name, transaction.subject),
          span(cls := Style.fidorId.name, s"(${transaction.id})")
          )
        }
      )
    )
  }

}
