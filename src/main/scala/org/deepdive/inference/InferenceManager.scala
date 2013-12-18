package org.deepdive.inference

import org.deepdive.calibration._
import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import java.io.File

/* Manages the Factor and Variable relations in the database */
trait InferenceManager extends Actor with ActorLogging {
  self: InferenceDataStoreComponent with CalibrationDataComponent =>
  
  val factorGraphBuilder = context.actorOf(FactorGraphBuilder.props)

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case msg : FactorGraphBuilder.AddFactorsAndVariables =>
      factorGraphBuilder forward msg
    case InferenceManager.WriteInferenceResult(file) =>
      log.info("writing inference result back to datastore")
      inferenceDataStore.writeInferenceResult(file)
      sender ! "Done"
    case InferenceManager.DumpFactorGraph(factorMapFile, factorsFile, weightsFile) =>
      log.info("dumping factor graph")
      inferenceDataStore.dumpFactorGraph(new File(factorMapFile), new File(factorsFile), 
        new File(weightsFile))
      sender ! "Done"
    case InferenceManager.WriteCalibrationData(countFilePrefix, precisionFilePrefix) =>
      log.info("writing calibration data")
      calibrationData.writeBucketCounts(countFilePrefix)
      calibrationData.writeBucketPrecision(precisionFilePrefix)
      sender ! "Done"
    case other =>
      log.warning("Huh?")
  }
}

object InferenceManager {

  // TODO: Refactor this
  class PostgresInferenceManager extends InferenceManager with 
    PostgresInferenceDataStoreComponent with PostgresCalibrationDataComponent

  def props : Props = Props(classOf[PostgresInferenceManager])

  // Messages
  case class WriteInferenceResult(file: String)
  case class DumpFactorGraph(factorMapFile: String, factorsFile: String, weightsFile: String)
  case class WriteCalibrationData(countFilePrefix: String, precisionFilePrefix: String)

}