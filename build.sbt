name := "deepdive"

version := "0.0.3"

scalaVersion := "2.10.3"

scalacOptions += "-feature"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray" at "http://repo.spray.io/"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= List(
  "commons-io" % "commons-io" % "2.0",
  "ch.qos.logback" % "logback-classic" % "1.0.7",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "com.h2database" % "h2" % "1.3.166",
  "com.netflix.rxjava" % "rxjava-scala" % "0.15.1",
  "com.typesafe" % "config" % "1.0.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3-M2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3-M2",
  "com.typesafe.akka" %% "akka-testkit" % "2.3-M2",
  "com.typesafe.atmos" % "trace-akka-2.2.1_2.10" % "1.3.0",
  "com.typesafe.play" %% "play-json" % "2.2.1",
  "mysql" % "mysql-connector-java" % "5.1.12",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC2" % "test",
  "org.scalikejdbc" %% "scalikejdbc" % "[1.7,)",
  "org.scalikejdbc" %% "scalikejdbc-config" % "[1.7,)",
  "play" %% "anorm" % "2.1.5"
  //"org.apache.hive" % "hive-jdbc" % "1.1.0",
  //"org.apache.hadoop" % "hadoop-common" % "2.7.0"
)

// Impala JDBC is not in maven central; we may want to set up our own 
// maven repository at some point to avoid unmanaged jars
unmanagedJars in Compile += file("lib/ImpalaJDBC41.jar")

unmanagedJars in Compile += file("lib/libthrift-0.9.0.jar")

parallelExecution in Test := false

packSettings

// [Optional: Mappings from a program name to the corresponding Main class ]
packMain := Map("deepdive" -> "org.deepdive.Main")

jacoco.settings

parallelExecution in jacoco.Config := false

