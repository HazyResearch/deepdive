name := "deepdive"

version := "0.1"

scalaVersion := "2.10.3"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= List(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "com.typesafe" % "config" % "1.0.2",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC2" % "test",
  "com.typesafe.slick" %% "slick" % "1.0.1",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.h2database" % "h2" % "1.3.166"
)