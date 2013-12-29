package org.deepdive.inference

import akka.actor._
import scala.sys.process._
import scala.util.{Success, Failure}

/* Companion object for the Sampler actor. Use the props method to create a new Sampler */
object Sampler {
  def props = Props[Sampler]

  // Tells the Sampler to run inference
  case class Run(samplerJavaArgs: String, samplerOptions: String, variablesPath: String, 
    factorsPath: String, weightsPath: String, variableOutPath: String)
}

/* Runs inferece on a dumped factor graph. */
class Sampler extends Actor with ActorLogging {

  override def preStart() = { log.info("starting") }

  def receive = {
    case Sampler.Run(samplerJavaArgs, samplerOptions, variablesPath, factorsPath, weightsPath, 
      variableOutPath) =>
      // Build the command
      val samplerCmd = buildSamplerCmd(samplerJavaArgs, samplerOptions, variablesPath, factorsPath, weightsPath, 
        variableOutPath)
      log.info(s"Executing: ${samplerCmd.mkString(" ")}")
      // We run the process, get its exit value, and print its output to the log
      val exitValue = samplerCmd!(ProcessLogger(
        out => log.debug(out),
        err => log.error(err)
      ))
      // Depending on the exitvalue we return success or throw an exception
      exitValue match {
        case 0 => sender ! Success()
        case _ => throw new RuntimeException("sampling failed (see error log)")
      }
      
  }

  def buildSamplerCmd(samplerJavaArgs: String, samplerOptions: String, variablesPath: String,
    factorsPath: String, weightsPath: String, variableOutPath: String) = {
    Seq("java", samplerJavaArgs, 
      "-jar", "lib/gibbs_sampling-assembly-0.1.jar", 
      "--variables", variablesPath, 
      "--factors", factorsPath, 
      "--weights", weightsPath,
      "--output", variableOutPath) ++ samplerOptions.split(" ")
  }

}

