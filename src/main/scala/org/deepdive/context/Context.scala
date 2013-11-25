package org.deepdive.context

import akka.actor.ActorSystem
import java.io.File

/* Describes the context of the DeepDive application */
object Context {

  lazy val system = ActorSystem("deepdive")
  var configFile : File = null

}