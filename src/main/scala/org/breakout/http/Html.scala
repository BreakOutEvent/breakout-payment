package org.breakout.http

import org.breakout.connector.fidor.FidorTransaction
import org.breakout.util.StringUtils._
import scalatags.Text
import scalatags.Text.all.{paddingLeft, span, _}
import scalatags.stylesheet._

object Html {

  object Style extends StyleSheet {
    initStyleSheet()

    val valid = cls(
      color := "green"
    )
    val invalid = cls(
      color := "red"
    )
    val fidorId = cls(
      color := "#c6c6c6",
      paddingLeft := "10px"
    )
    val subject = cls(
      paddingLeft := "10px"
    )
    val date = cls(
      color := "#000"
    )
  }

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
