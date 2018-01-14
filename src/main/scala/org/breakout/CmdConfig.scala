package org.breakout

import org.breakout.DryRunOption._
import org.breakout.Modes._
import org.breakout.UsageEnvironment._

case class CmdConfig(mode: Mode = NONE, dryRun: DryRun = DRY_RUN, usageEnvironment: UsageEnvironment = CLI)

case class Mode(value: String)

object Modes {
  val NONE = Mode("none")
  val CHECK_PAID = Mode("checkPaid")
}


case class DryRun(enabled: Boolean, name: String)

object DryRunOption {
  val DRY_RUN = DryRun(enabled = true, "WITH DRY-RUN")
  val NO_DRY_RUN = DryRun(enabled = false, "WITHOUT DRY-RUN")
}

case class UsageEnvironment(value: String)

object UsageEnvironment {
  val CLI = UsageEnvironment("CLI")
  val WEB_FRONTEND = UsageEnvironment("WEB_FRONTEND")
}