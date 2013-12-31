package org.deepdive.inference

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.io.File
import org.deepdive.TaskManager
import org.deepdive.calibration._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.{Try, Success, Failure}

/* Manages the Factor and Variable relations in the database */
trait InferenceManager extends Actor with ActorLogging {
  self: InferenceDataStoreComponent with CalibrationDataComponent =>
  
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

  lazy val VariablesDumpFile = new File("target/variables.tsv")
  lazy val FactorsDumpFile = new File("target/factors.tsv")
  lazy val WeightsDumpFile = new File("target/weights.tsv")
  lazy val SamplingOutputFile = new File("target/inference_result.out")

  val factorGraphBuilder = context.actorOf(factorGraphBuilderProps)

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case msg @ FactorTask(factorDesc, holdoutFraction) =>
      val result = factorGraphBuilder ? FactorGraphBuilder.AddFactorsAndVariables(
        factorDesc, holdoutFraction) 
      result.mapTo[Try[Unit]] pipeTo sender
    case InferenceManager.RunInference(samplerJavaArgs, samplerOptions) =>
      val _sender = sender
      val result = runInference(samplerJavaArgs, samplerOptions)
      result onComplete { maybeResult => _sender ! maybeResult } 
    case InferenceManager.WriteCalibrationData(countFilePrefix, precisionFilePrefix) =>
      log.info("writing calibration data")
      calibrationData.writeBucketCounts(countFilePrefix)
      calibrationData.writeBucketPrecision(precisionFilePrefix)
      sender ! Success()
  }

  def runInference(samplerJavaArgs: String, samplerOptions: String) = {
    inferenceDataStore.dumpFactorGraph(VariablesDumpFile, FactorsDumpFile, WeightsDumpFile)
    val sampler = context.actorOf(Sampler.props, "sampler")
    val samplingResult = sampler ? Sampler.Run(samplerJavaArgs, samplerOptions,
      VariablesDumpFile.getCanonicalPath, FactorsDumpFile.getCanonicalPath, 
      WeightsDumpFile.getCanonicalPath, SamplingOutputFile.getCanonicalPath)
    // Kill the sampler after it's done :)
    sampler ! PoisonPill
    samplingResult.map { x =>
      inferenceDataStore.writebackInferenceResult(SamplingOutputFile.getCanonicalPath)
    }
  }

}

object InferenceManager {

  class PostgresInferenceManager(val taskManager: ActorRef, val variableSchema: Map[String, String]) 
    extends InferenceManager with PostgresInferenceDataStoreComponent 
    with PostgresCalibrationDataComponent {
    
    def factorGraphBuilderProps = 
      Props(classOf[FactorGraphBuilder.PostgresFactorGraphBuilder], variableSchema)
  }

  // TODO: Refactor this to take the data store type as an argument
  def props(taskManager: ActorRef, variableSchema: Map[String, String]) = 
    Props(classOf[PostgresInferenceManager], taskManager, variableSchema: Map[String, String])

  // Messages
  case class RunInference(samplerJavaArgs: String, samplerOptions: String)
  case class WriteCalibrationData(countFilePrefix: String, precisionFilePrefix: String)

}