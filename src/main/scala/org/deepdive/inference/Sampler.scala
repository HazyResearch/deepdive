package org.deepdive.inference

import akka.actor._
import scala.sys.process._
import scala.util.{Success, Failure}

/* Companion object for the Sampler actor. Use the props method to create a new Sampler */
object Sampler {
  def props = Props[Sampler]

  // Tells the Sampler to run inference
  case class Run(samplerCmd: String, samplerOptions: String, weightsFile: String, variablesFile: String,
    factorsFile: String, edgesFile: String, metaFile: String, outputDir: String, parallelGrounding: Boolean)
}

/* Runs inferece on a dumped factor graph. */
class Sampler extends Actor with ActorLogging {

  override def preStart() = { log.info("starting") }

  def receive = {
    case Sampler.Run(samplerCmd, samplerOptions, weightsFile, variablesFile,
      factorsFile, edgesFile, metaFile, outputDir, parallelGrounding) =>
      // Build the command
      val cmd = buildSamplerCmd(samplerCmd, samplerOptions, weightsFile, variablesFile,
      factorsFile, edgesFile, metaFile, outputDir, parallelGrounding)
      log.info(s"Executing: ${cmd.mkString(" ")}")
      
      // Handle the case where cmd! throw exception rather than return a value
      try {
        // We run the process, get its exit value, and print its output to the log file
        val exitValue = cmd!(ProcessLogger(
          out => log.info(out),
          err => System.err.println(err)
        ))        
        // Depending on the exit value we return success or kill the program
        exitValue match {
          case 0 => sender ! Success()
          case _ => {
            import scala.sys.process._
            import java.lang.management
            import sun.management.VMManagement;
            import java.lang.management.ManagementFactory;
            import java.lang.management.RuntimeMXBean;
            import java.lang.reflect.Field;
            import java.lang.reflect.Method;
            var pid = ManagementFactory.getRuntimeMXBean().getName().toString
            val pattern = """\d+""".r
            pattern.findAllIn(pid).foreach(id => s"kill -9 ${id}".!)
          }
        }
      } catch {
        // If some exception is thrown, terminate DeepDive
        case e: Throwable =>
        sender ! Status.Failure(e)
        context.stop(self)
      }

  }

  // Build the command to run the sampler
  def buildSamplerCmd(samplerCmd: String, samplerOptions: String, weightsFile: String, 
    variablesFile: String, factorsFile: String, edgesFile: String, metaFile: String, 
    outputDir: String, parallelGrounding: Boolean) = {
    log.info(samplerCmd)
    samplerCmd.split(" ").toSeq ++ Seq(
      "-w", weightsFile,
      "-v", variablesFile,
      "-f", factorsFile,
      "-e", edgesFile,
      "-m", metaFile,
      "-o", outputDir) ++ samplerOptions.split(" ")
  }

}
