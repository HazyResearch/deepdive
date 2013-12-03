package org.deepdive.context

import akka.actor.ActorSystem
import java.io.File
import org.deepdive.settings._

/* Describes the context of the DeepDive application */
object Context {

  lazy val system = ActorSystem("deepdive")
  var configFile : File = null
  var settings : Settings = null

}