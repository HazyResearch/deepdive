scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature")

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

resolvers += Resolver.sonatypeRepo("public")
