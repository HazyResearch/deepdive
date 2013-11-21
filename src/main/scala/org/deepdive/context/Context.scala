package org.deepdive.context

import akka.actor.ActorSystem

/* Describes the context of the DeepDive application */
object Context {

  lazy val system = ActorSystem("deepdive")

}