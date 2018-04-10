package org.breakout.http.html

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
    a(href := fidorLink, "Fidor Zugriff authorisieren")

  def transactionsPage(transactions: Seq[FidorTransaction]): Text.TypedTag[String] = {

    def transactionClass(transaction: FidorTransaction) = transaction.subject.hasValidSubject match {
      case true => Style.valid
      case false => Style.invalid
    }

    div(
      h2("Legende:"),
      div(cls := Style.valid.name, "Code gültig"),
      div(cls := Style.invalid.name, "Code ungültig"),
      h2("Transaktionen:"),
      a(cls := Style.backend.name, href := "/transfer", "Gültige an Backend übertragen"),
      ul(
        transactions.map(transaction => li(
          cls := transactionClass(transaction).name,
          span(cls := Style.date.name, transaction.value_date.getOrElse("").toString),
          span(cls := Style.subject.name, transaction.subject),
          span(cls := Style.fidorId.name, s"(${transaction.id})")
        ))
      )
    )
  }

}
