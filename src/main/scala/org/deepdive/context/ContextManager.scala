package org.deepdive.context

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

object ContextManager {
  def props(): Props = Props(classOf[ContextManager])
}

class ContextManager(initialContext: Context) extends Actor with ActorLogging {

  case class GetContext
  
  override def preStart() {
    log.debug("Starting")
  }

  def receive = {
    case GetContext =>
      sender ! initialContext
  }

}