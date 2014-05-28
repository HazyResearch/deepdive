package org.deepdive.inference

import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.io.File
import org.deepdive.TaskManager
import org.deepdive.calibration._
import org.deepdive.settings.{FactorDesc, VariableDataType}
import org.deepdive.Context
import org.deepdive.settings._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.Try

/* Manages the Factor and Variable relations in the database */
trait InferenceManager extends Actor with ActorLogging {
  self: InferenceDataStoreComponent =>
  
  implicit val taskTimeout = Timeout(200 hours)
  import context.dispatcher

  def dbSettings: DbSettings
  // All variables used in the system with their types
  def variableSchema: Map[String, _ <: VariableDataType]
  // Reference to the task manager
  def taskManager: ActorRef
  // Described how to start the factor graph builder
  def factorGraphBuilderProps : Props
  // Describes how to start the sampler
  def samplerProps : Props = Sampler.props
  // Describes how to start the calibration data writer
  def calibrationDataWriterProps = CalibrationDataWriter.props

  lazy val factorGraphDumpFileWeights = new File(s"${Context.outputDir}/graph.weights")
  lazy val factorGraphDumpFileVariables = new File(s"${Context.outputDir}/graph.variables")
  lazy val factorGraphDumpFileFactors = new File(s"${Context.outputDir}/graph.factors")
  lazy val factorGraphDumpFileEdges = new File(s"${Context.outputDir}/graph.edges")
  lazy val factorGraphDumpFileMeta = new File(s"${Context.outputDir}/graph.meta.csv")
  lazy val SamplingOutputDir = new File(s"${Context.outputDir}")
  lazy val SamplingOutputFile = new File(s"${SamplingOutputDir}/inference_result.out")
  lazy val SamplingOutputFileWeights = new File(s"${SamplingOutputDir}/inference_result.out.weights")

  val factorGraphBuilder = context.actorOf(factorGraphBuilderProps, "factorGraphBuilder")

  override def preStart() {
    log.info("Starting")
    inferenceDataStore.init()
    context.watch(factorGraphBuilder)
  }

  override val supervisorStrategy = OneForOneStrategy() {
    case _ : Exception => Escalate
  }

  def receive = {
    case InferenceManager.GroundFactorGraph(factorDescs, holdoutFraction, holdoutQuery, skipLearning, weightTable) =>
      val _sender = sender
      inferenceDataStore.asInstanceOf[SQLInferenceDataStore]
        .groundFactorGraph(variableSchema, factorDescs, holdoutFraction, holdoutQuery, skipLearning, weightTable, dbSettings)
      sender ! "OK"
      // factorGraphBuilder ? FactorGraphBuilder.AddFactorsAndVariables(
      //   factorDesc, holdoutFraction, batchSize) pipeTo _sender
    case InferenceManager.RunInference(factorDescs, holdoutFraction, holdoutQuery, samplerJavaArgs, samplerOptions, skipSerializing) =>
      val _sender = sender
      val result = runInference(factorDescs, holdoutFraction, holdoutQuery, samplerJavaArgs, samplerOptions, skipSerializing)
      result pipeTo _sender
    case InferenceManager.WriteCalibrationData =>
      val _sender = sender
      log.info("writing calibration data")
      val calibrationWriter = context.actorOf(calibrationDataWriterProps)
      // Get and write calibraton data for each variable
      val futures = variableSchema.map { case(variable, dataType) =>
        val filename = s"${Context.outputDir}/calibration/${variable}.tsv"
        val data = inferenceDataStore.getCalibrationData(variable, dataType, Bucket.ten)
        calibrationWriter ? CalibrationDataWriter.WriteCalibrationData(filename, data)
      }
      Future.sequence(futures) pipeTo _sender
      calibrationWriter ! PoisonPill
  }

  def runInference(factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String], 
    samplerJavaArgs: String, samplerOptions: String, skipSerializing: Boolean = false) = {
    // TODO: Make serializier configurable
    skipSerializing match {
      case false =>
        factorGraphDumpFileMeta.getParentFile().mkdirs()
        // factorGraphDumpFileWeights.createNewFile()
        // factorGraphDumpFileVariables.createNewFile()
        // factorGraphDumpFileFactors.createNewFile()
        // factorGraphDumpFileEdges.createNewFile()
        // factorGraphDumpFileMeta.createNewFile()
        val weightsOutput = new java.io.BufferedOutputStream(new java.io.FileOutputStream(factorGraphDumpFileWeights, false))
        val variablesOutput = new java.io.BufferedOutputStream(new java.io.FileOutputStream(factorGraphDumpFileVariables, false))
        val factorsOutput = new java.io.BufferedOutputStream(new java.io.FileOutputStream(factorGraphDumpFileFactors, false))
        val edgesOutput = new java.io.BufferedOutputStream(new java.io.FileOutputStream(factorGraphDumpFileEdges, false))
        val metaOutput = new java.io.BufferedOutputStream(new java.io.FileOutputStream(factorGraphDumpFileMeta, false))
        val serializier = new BinarySerializer(weightsOutput, variablesOutput, factorsOutput, edgesOutput, metaOutput)
        inferenceDataStore.dumpFactorGraph(serializier, variableSchema, factorDescs, holdoutFraction,
          holdoutQuery, factorGraphDumpFileWeights.getCanonicalPath,
          factorGraphDumpFileVariables.getCanonicalPath, factorGraphDumpFileFactors.getCanonicalPath,
          factorGraphDumpFileEdges.getCanonicalPath)
        serializier.close()
        weightsOutput.close()
        variablesOutput.close()
        factorsOutput.close()
        metaOutput.close()
      case true =>
    }
    val sampler = context.actorOf(samplerProps, "sampler")
    val samplingResult = sampler ? Sampler.Run(samplerJavaArgs, samplerOptions,
      factorGraphDumpFileWeights.getCanonicalPath, factorGraphDumpFileVariables.getCanonicalPath,
      factorGraphDumpFileFactors.getCanonicalPath, factorGraphDumpFileEdges.getCanonicalPath,
      factorGraphDumpFileMeta.getCanonicalPath, SamplingOutputDir.getCanonicalPath)
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
  class PostgresInferenceManager(val taskManager: ActorRef, val variableSchema: Map[String, _ <: VariableDataType], val dbSettings: DbSettings) 
    extends InferenceManager with PostgresInferenceDataStoreComponent {
    def factorGraphBuilderProps = 
      Props(classOf[FactorGraphBuilder.PostgresFactorGraphBuilder], variableSchema)
  }

  // TODO: Refactor this to take the data store type as an argument
  def props(taskManager: ActorRef, variableSchema: Map[String, _ <: VariableDataType],
    dbSettings: DbSettings) = {
    dbSettings.driver match {
       case "org.postgresql.Driver" => Props(classOf[PostgresInferenceManager], taskManager, variableSchema, dbSettings)
    }
  }
    

  // Messages
  // ==================================================

  // Executes a task to build part of the factor graph
  case class GroundFactorGraph(factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String)
  // Runs the sampler with the given arguments
  case class RunInference(factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String], samplerJavaArgs: String, samplerOptions: String, skipSerializing: Boolean = false)
  // Writes calibration data to predefined files
  case object WriteCalibrationData

}
