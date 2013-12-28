package org.deepdive.inference

import akka.actor._
import scala.sys.process._
import scala.util.Success

object Sampler {
  def props = Props[Sampler]
  case class Run(samplerCmd: Seq[String])
}

class Sampler extends Actor with ActorLogging {

  override def preStart() = { log.info("starting") }

  def receive = {
    case Sampler.Run(samplerCmd) =>
      log.info(s"Executing: ${samplerCmd.mkString(" ")}")
      val samplerOutput = samplerCmd.!!
      log.debug(samplerOutput)
      log.info(s"sampling finished.")
      sender ! Success()
  }

}

