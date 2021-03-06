package org.breakout.http.html

import scalatags.Text.all._
import scalatags.stylesheet.StyleSheet

object Style extends StyleSheet {
  initStyleSheet()

  val valid = cls(
    color.green
  )
  val validNew = cls(
    color.blue
  )
  val invalidUnknown = cls(
    color.teal
  )
  val invalid = cls(
    color.red
  )
  val fidorId = cls(
    color.gray,
    paddingLeft := "10px"
  )
  val subject = cls(
    paddingLeft := "10px",
    paddingRight := "10px"
  )
  val date = cls(
    paddingLeft := "5px",
    color.black
  )
  val backend = cls(
    border := "1px solid black",
    borderRadius := "5px",
    padding := "5px",
    textDecoration.none,
    color.black
  )
}