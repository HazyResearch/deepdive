package org.deepdive.inference

import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.io.File
import org.deepdive.TaskManager
import org.deepdive.calibration._
import org.deepdive.settings.FactorDesc
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.Try

/* Manages the Factor and Variable relations in the database */
trait InferenceManager extends Actor with ActorLogging {
  self: InferenceDataStoreComponent =>
  
  implicit val taskTimeout = Timeout(24 hours)
  import context.dispatcher

  // All variables used in the system with their types
  def variableSchema: Map[String, String]
  // Reference to the task manager
  def taskManager: ActorRef
  // Described how to start the factor graph builder
  def factorGraphBuilderProps : Props
  // Describes how to start the sampler
  def samplerProps : Props = Sampler.props
  // Describes how to start the calibration data writer
  def calibrationDataWriterProps = CalibrationDataWriter.props

  lazy val VariablesDumpFile = new File("target/variables.txt")
  lazy val FactorsDumpFile = new File("target/factors.txt")
  lazy val WeightsDumpFile = new File("target/weights.txt")
  lazy val SamplingOutputFile = new File("target/inference_result.out")
  lazy val SamplingOutputFileWeights = new File("target/inference_result.out.weights")

  val factorGraphBuilder = context.actorOf(factorGraphBuilderProps, "factorGraphBuilder")

  override def preStart() {
    log.info("Starting")
    context.watch(factorGraphBuilder)
    inferenceDataStore.init()
  }

  override val supervisorStrategy = OneForOneStrategy() {
    case _ : Exception => Escalate
  }

  def receive = {
    case InferenceManager.FactorTask(factorDesc, holdoutFraction, batchSize) =>
      val _sender = sender
      factorGraphBuilder ? FactorGraphBuilder.AddFactorsAndVariables(
        factorDesc, holdoutFraction, batchSize) pipeTo _sender
    case InferenceManager.RunInference(samplerJavaArgs, samplerOptions) =>
      val _sender = sender
      val result = runInference(samplerJavaArgs, samplerOptions)
      result pipeTo _sender
    case InferenceManager.WriteCalibrationData =>
      val _sender = sender
      log.info("writing calibration data")
      val calibrationWriter = context.actorOf(calibrationDataWriterProps)
      // Get and write calibraton data for each variable
      val futures = variableSchema.keys.map { variable =>
        val filename = s"target/calibration/${variable}.tsv"
        val data = inferenceDataStore.getCalibrationData(variable, Bucket.ten)
        calibrationWriter ? CalibrationDataWriter.WriteCalibrationData(filename, data)
      }
      Future.sequence(futures) pipeTo _sender
      calibrationWriter ! PoisonPill
  }

  def runInference(samplerJavaArgs: String, samplerOptions: String) = {
    inferenceDataStore.dumpFactorGraph(VariablesDumpFile, FactorsDumpFile, WeightsDumpFile)
    val sampler = context.actorOf(samplerProps, "sampler")
    val samplingResult = sampler ? Sampler.Run(samplerJavaArgs, samplerOptions,
      VariablesDumpFile.getCanonicalPath, FactorsDumpFile.getCanonicalPath, 
      WeightsDumpFile.getCanonicalPath, SamplingOutputFile.getCanonicalPath)
    // Kill the sampler after it's done :)
    sampler ! PoisonPill
    samplingResult.map { x =>
      inferenceDataStore.writebackInferenceResult(
        variableSchema, SamplingOutputFile.getCanonicalPath, 
        SamplingOutputFileWeights.getCanonicalPath)
    }
  }

}

object InferenceManager {

  /* An inference manager that uses postgres as its datastore */
  class PostgresInferenceManager(val taskManager: ActorRef, val variableSchema: Map[String, String]) 
    extends InferenceManager with PostgresInferenceDataStoreComponent {
    
    def factorGraphBuilderProps = 
      Props(classOf[FactorGraphBuilder.PostgresFactorGraphBuilder], variableSchema)
  }

  // TODO: Refactor this to take the data store type as an argument
  def props(
    taskManager: ActorRef, variableSchema: Map[String, String]) = 
    Props(classOf[PostgresInferenceManager], taskManager, variableSchema: Map[String, String])

  // Messages
  // ==================================================

  // Executes a task to build part of the factor graph
  case class FactorTask(factorDesc: FactorDesc, holdoutFraction: Double, batchSize: Option[Int])
  // Runs the sampler with the given arguments
  case class RunInference(samplerJavaArgs: String, samplerOptions: String)
  // Writes calibration data to predefined files
  case object WriteCalibrationData

}