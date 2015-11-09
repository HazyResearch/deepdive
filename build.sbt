name := "deepdive"

version := "0.7.1"

scalaVersion := "2.10.5"

scalacOptions += "-feature"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray" at "http://repo.spray.io/"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= List(
  "commons-io" % "commons-io" % "2.0",
  "ch.qos.logback" % "logback-classic" % "1.0.7",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "com.netflix.rxjava" % "rxjava-scala" % "0.15.1",
  "com.typesafe" % "config" % "1.0.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3-M2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3-M2",
  "com.typesafe.akka" %% "akka-testkit" % "2.3-M2",
  //"com.typesafe.atmos" % "trace-akka-2.2.1_2.10" % "1.3.0",
  "com.typesafe.play" %% "play-json" % "2.2.1",
  "mysql" % "mysql-connector-java" % "5.1.12",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.postgresql" % "postgresql" % "9.4-1203-jdbc4",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC2" % "test",
  "org.scalikejdbc" %% "scalikejdbc" % "[1.7,)",
  "org.scalikejdbc" %% "scalikejdbc-config" % "[1.7,)",
  "org.apache.commons" % "commons-dbcp2" % "2.1.1",
  "play" %% "anorm" % "2.1.5"
)

parallelExecution in Test := false

// print defined tests
val printTests = taskKey[Unit]("printTests")

printTests := {
  val tests = (definedTests in Test).value
  tests map { t =>
    println(t.name)
  }
}

test in assembly := {}

mainClass in assembly := Some("com.example.Main")
