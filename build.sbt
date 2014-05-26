name := "deepdive"

version := "0.0.3"

scalaVersion := "2.10.3"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray" at "http://repo.spray.io/"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= List(
  "commons-io" % "commons-io" % "2.0",
  "ch.qos.logback" % "logback-classic" % "1.0.7",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "com.google.protobuf" % "protobuf-java" % "2.5.0",
  "com.h2database" % "h2" % "1.3.166",
  "com.netflix.rxjava" % "rxjava-scala" % "0.15.1",
  "com.typesafe" % "config" % "1.0.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3-M2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3-M2",
  "com.typesafe.akka" %% "akka-testkit" % "2.3-M2",
  "com.typesafe.atmos" % "trace-akka-2.2.1_2.10" % "1.3.0",
  "com.typesafe.play" %% "play-json" % "2.2.1",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC2" % "test",
  "org.scalikejdbc" %% "scalikejdbc" % "[1.7,)",
  "org.scalikejdbc" %% "scalikejdbc-config" % "[1.7,)",
  "play" %% "anorm" % "2.1.5"
)

parallelExecution in Test := false

packSettings

// [Optional: Mappings from a program name to the corresponding Main class ]
packMain := Map("deepdive" -> "org.deepdive.Main")