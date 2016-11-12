
name := "breakout-payment"
organization := "org.breakout"
version := "0.1.0"
mainClass := Some("org.breakout.Main")
scalaVersion := "2.12.0"

libraryDependencies ++= {

  val scoptVersion = "3.5.0"
  val sprayJsonVersion = "1.3.2"
  val logBackVersion = "1.1.7"
  val scalaLoggingVersion = "3.5.0"
  val configVersion = "1.3.1"

  Seq(
    "com.github.scopt"            %% "scopt"           % scoptVersion,
    "io.spray"                    %% "spray-json"      % sprayJsonVersion,
    "ch.qos.logback"              %  "logback-classic" % logBackVersion,
    "com.typesafe.scala-logging"  %% "scala-logging"   % scalaLoggingVersion,
    "com.typesafe"                %  "config"          % configVersion
  )
}