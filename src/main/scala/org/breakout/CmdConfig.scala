package org.breakout

import org.breakout.DryRunOption._
import org.breakout.Modes._

case class CmdConfig(mode: Mode = NONE, dryRun: DryRun = DRY_RUN)


case class Mode(value: String)

object Modes {
  val NONE = Mode("none")
  val CHECK_PAID = Mode("checkPaid")
}


case class DryRun(value: Boolean, name: String)

object DryRunOption {
  val DRY_RUN = DryRun(value = true, "WITH DRY-RUN")
  val NO_DRY_RUN = DryRun(value = false, "WITHOUT DRY-RUN")
}