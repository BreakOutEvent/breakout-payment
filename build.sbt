
name := "breakout-payment"
organization := "org.breakout"
version := "0.1.0"
mainClass := Some("org.breakout.Main")
scalaVersion := "2.11.8"

libraryDependencies ++= {

  val scoptVersion = "3.5.0"
  val sprayVersion = "1.3.2"
  val logBackVersion = "1.1.7"
  val scalaLoggingVersion = "3.5.0"
  val configVersion = "1.3.1"
  val akkaVersion = "2.4.12"

  Seq(
    "com.github.scopt"            %% "scopt"           % scoptVersion,
    "io.spray"                    %% "spray-json"      % sprayVersion,
    "io.spray"                    %% "spray-client"    % sprayVersion,
    "ch.qos.logback"              %  "logback-classic" % logBackVersion,
    "com.typesafe.scala-logging"  %% "scala-logging"   % scalaLoggingVersion,
    "com.typesafe"                %  "config"          % configVersion,
    "com.typesafe.akka"           %% "akka-actor"      % akkaVersion
  )
}