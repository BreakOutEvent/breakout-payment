
name := "breakout-payment"
organization := "org.breakout"
version := "1.2.0"
mainClass := Some("org.breakout.Main")
scalaVersion := "2.11.8"

libraryDependencies ++= {

  val scoptVersion = "3.7.0"
  val sprayVersion = "1.3.4"
  val logBackVersion = "1.2.3"
  val scalaLoggingVersion = "3.7.2"
  val configVersion = "1.3.2"
  val akkaVersion = "2.5.9"
  val http4sVersion = "0.17.6"
  val scalaTagsVersion = "0.6.7"

  Seq(
    "com.github.scopt"            %% "scopt"               % scoptVersion,
    "io.spray"                    %% "spray-json"          % sprayVersion,
    "io.spray"                    %% "spray-client"        % sprayVersion,
    "ch.qos.logback"              %  "logback-classic"     % logBackVersion,
    "com.typesafe.scala-logging"  %% "scala-logging"       % scalaLoggingVersion,
    "com.typesafe"                %  "config"              % configVersion,
    "com.typesafe.akka"           %% "akka-actor"          % akkaVersion,
    "org.http4s"                  %% "http4s-dsl"          % http4sVersion,
    "org.http4s"                  %% "http4s-blaze-server" % http4sVersion,
    "org.http4s"                  %% "http4s-blaze-client" % http4sVersion,
    "com.lihaoyi"                 %% "scalatags"           % scalaTagsVersion
  )
}