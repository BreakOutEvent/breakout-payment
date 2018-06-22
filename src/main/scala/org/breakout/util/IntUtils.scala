package org.breakout.util

import com.typesafe.scalalogging.Logger

import scala.math.BigDecimal.RoundingMode

object IntUtils {

  private val log = Logger[IntUtils.type]

  implicit class IntImprovements(val int: Int) {
    def toDecimalAmount: BigDecimal = {
      (BigDecimal(int) / 100).setScale(2, RoundingMode.HALF_UP)
    }

    def toEuroString: String = {
      s"${int.toDecimalAmount} €".replace(".", ",")
    }
  }

}