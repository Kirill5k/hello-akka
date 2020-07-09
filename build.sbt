ThisBuild / name := "hello-akka"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.3"

lazy val akkaVersion = "2.5.25"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.1.1" % "test"
)
