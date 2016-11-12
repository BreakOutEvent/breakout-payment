package org.breakout.logic

import com.typesafe.scalalogging.Logger
import org.breakout.CmdConfig


object CheckPaidLogic {

  val log = Logger[CheckPaidLogic.type]

  def doPaidCheck(cmdConfig: CmdConfig) = {
    log.info(s"Start doPaidCheck ${cmdConfig.dryRun.name}")
  }
}
