package org.deepdive.context

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

object ContextManager {
  def props(): Props = Props(classOf[ContextManager])
}

class ContextManager extends Actor with ActorLogging {

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case _ => log.debug("I got a message ;)")
  }

}