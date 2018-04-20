package org.breakout

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.breakout.DryRunOption.NO_DRY_RUN
import org.breakout.Modes._
import org.breakout.UsageEnvironment._
import org.breakout.http.Frontend
import org.breakout.logic.CheckPaidLogic
import scopt.OptionParser

object Main extends App {
  val config = ConfigFactory.load()

  val parser = new OptionParser[CmdConfig]("breakout-payment") {
    head(config.getString("name"), config.getString("version"))

    opt[Unit]("no-dry-run")
      .action((_, c) => c.copy(dryRun = NO_DRY_RUN)).text("flags to not do a dry-run")

    help("help").text("prints this usage text")

    note("")

    cmd(CHECK_PAID.value)
      .action((_, c) => c.copy(mode = CHECK_PAID))
      .text("checks weather payments the backend expects were received.")
  }

  parser.parse(args, CmdConfig()).foreach(cmdConfig => cmdConfig.mode match {
    case NONE => runFrontend()
    case CHECK_PAID => CheckPaidLogic.doPaidCheck(cmdConfig.copy(usageEnvironment = CLI))
    case notImplementedMode => throw new RuntimeException(s"$notImplementedMode is not implemented yet!")
  })

  def runFrontend(): Unit = {
    ActorSystem("web-frontend")

    Frontend.runWebServer()


  }
}
