package org.deepdive.inference

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.io.File
import org.deepdive.TaskManager
import org.deepdive.calibration._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

/* Manages the Factor and Variable relations in the database */
trait InferenceManager extends Actor with ActorLogging {
  self: InferenceDataStoreComponent with CalibrationDataComponent =>
  
  implicit val taskTimeout = Timeout(24 hours)
  import context.dispatcher

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
    case InferenceManager.WriteInferenceResult(file) =>
      log.info("writing inference result back to datastore")
      inferenceDataStore.writeInferenceResult(file)
      sender ! Success()
    case InferenceManager.DumpFactorGraph(factorMapFile, factorsFile, weightsFile) =>
      log.info("dumping factor graph")
      inferenceDataStore.dumpFactorGraph(new File(factorMapFile), new File(factorsFile), 
        new File(weightsFile))
      sender ! Success()
    case InferenceManager.RunSampler(cmd) =>
      val sampler = context.actorOf(Sampler.props, "sampler")
      sampler ? Sampler.Run(cmd) pipeTo sender
    case InferenceManager.WriteCalibrationData(countFilePrefix, precisionFilePrefix) =>
      log.info("writing calibration data")
      calibrationData.writeBucketCounts(countFilePrefix)
      calibrationData.writeBucketPrecision(precisionFilePrefix)
      sender ! Success()
    case other =>
      log.warning("Huh?")
  }
}

object InferenceManager {

  // TODO: Refactor this
  class PostgresInferenceManager(val variableSchema: Map[String, String]) extends InferenceManager with 
    PostgresInferenceDataStoreComponent with PostgresCalibrationDataComponent

  def props(variableSchema: Map[String, String]) : Props = Props(classOf[PostgresInferenceManager], 
    variableSchema: Map[String, String])

  // Messages
  case class WriteInferenceResult(file: String)
  case class DumpFactorGraph(factorMapFile: String, factorsFile: String, weightsFile: String)
  case class WriteCalibrationData(countFilePrefix: String, precisionFilePrefix: String)
  case class RunSampler(cmd: Seq[String])

}