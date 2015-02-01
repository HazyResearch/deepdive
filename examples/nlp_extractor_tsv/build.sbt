import com.typesafe.sbt.SbtStartScript

name := "deepdive-nlp-parser"

version := "0.1"

scalaVersion := "2.10.3"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= List(
  "ch.qos.logback" % "logback-classic" % "1.0.7",
  "com.typesafe.play" %% "play-json" % "2.2.1",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1" classifier "models",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC2" % "test"
)

parallelExecution in Test := false

seq(SbtStartScript.startScriptForClassesSettings: _*)