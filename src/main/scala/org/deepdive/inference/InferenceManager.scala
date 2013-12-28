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

  lazy val VariablesDumpFile = new File("target/variables.tsv")
  lazy val FactorsDumpFile = new File("target/factors.tsv")
  lazy val WeightsDumpFile = new File("target/weights.tsv")
  lazy val SamplingOutputFile = new File("target/inference_result.out")

  def variableSchema: Map[String, String]

  val taskManager = context.actorSelection("../taskManager")
  val factorGraphBuilder = context.actorOf(FactorGraphBuilder.props(variableSchema))

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case msg @ FactorTask(factorDesc, holdoutFraction) =>
      val result = factorGraphBuilder ? FactorGraphBuilder.AddFactorsAndVariables(
        factorDesc, holdoutFraction) 
      result.mapTo[Try[Unit]] pipeTo sender
    case InferenceManager.RunInference(samplerJavaArgs, samplerOptions) =>
      val result = runInference(samplerJavaArgs, samplerOptions)
      result pipeTo sender
    case InferenceManager.WriteCalibrationData(countFilePrefix, precisionFilePrefix) =>
      log.info("writing calibration data")
      calibrationData.writeBucketCounts(countFilePrefix)
      calibrationData.writeBucketPrecision(precisionFilePrefix)
      sender ! Success()
  }

  def runInference(samplerJavaArgs: String, samplerOptions: String) : Future[Try[_]] = {
    inferenceDataStore.dumpFactorGraph(VariablesDumpFile, FactorsDumpFile, WeightsDumpFile)
    val sampler = context.actorOf(Sampler.props, "sampler")
    val samplingResult = sampler ? Sampler.Run(buildSamplerCmd(samplerJavaArgs, samplerOptions))
    sampler ! PoisonPill
    samplingResult.map { x =>
      Try(inferenceDataStore.writeInferenceResult(SamplingOutputFile.getCanonicalPath))
    }
  }

  private def buildSamplerCmd(samplerJavaArgs: String, samplerOptions: String) = {
    Seq("java", samplerJavaArgs, 
      "-jar", "lib/gibbs_sampling-assembly-0.1.jar", 
      "--variables", VariablesDumpFile.getCanonicalPath, 
      "--factors", FactorsDumpFile.getCanonicalPath, 
      "--weights", WeightsDumpFile.getCanonicalPath,
      "--output", SamplingOutputFile.getCanonicalPath) ++ samplerOptions.split(" ")
  }

}

object InferenceManager {

  // TODO: Refactor this
  class PostgresInferenceManager(val variableSchema: Map[String, String]) extends InferenceManager with 
    PostgresInferenceDataStoreComponent with PostgresCalibrationDataComponent

  def props(variableSchema: Map[String, String]) : Props = Props(classOf[PostgresInferenceManager], 
    variableSchema: Map[String, String])

  // Messages
  case class RunInference(samplerJavaArgs: String, samplerOptions: String)
  case class WriteCalibrationData(countFilePrefix: String, precisionFilePrefix: String)

}