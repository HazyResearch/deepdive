package org.deepdive.inference

import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.io.File
import org.deepdive.TaskManager
import org.deepdive.settings.{FactorDesc, VariableDataType}
import org.deepdive.Context
import org.deepdive.settings._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.language.postfixOps
import scala.util.Try

/* Manages the Factor and Variable relations in the database */
trait InferenceManager extends Actor with ActorLogging {
  self: InferenceRunnerComponent =>

  implicit val taskTimeout = Timeout(200 hours)
  import context.dispatcher

  def dbSettings: DbSettings
  // All variables used in the system with their types
  def variableSchema: Map[String, _ <: VariableDataType]
  // Reference to the task manager
  def taskManager: ActorRef
  // Describes how to start the sampler
  def samplerProps : Props = Sampler.props

  lazy val factorGraphDumpFileWeights = new File(s"${Context.outputDir}/graph.weights")
  lazy val factorGraphDumpFileVariables = new File(s"${Context.outputDir}/graph.variables")
  lazy val factorGraphDumpFileFactors = new File(s"${Context.outputDir}/graph.factors")
  lazy val factorGraphDumpFileEdges = new File(s"${Context.outputDir}/graph.edges")
  lazy val factorGraphDumpFileMeta = new File(s"${Context.outputDir}/graph.meta")
  lazy val SamplingOutputDir = new File(s"${Context.outputDir}")
  lazy val SamplingOutputFile = new File(s"${SamplingOutputDir}/inference_result.out.text")
  lazy val SamplingOutputFileWeights = new File(s"${SamplingOutputDir}/inference_result.out.weights.text")

  override def preStart() {
    log.info("Starting")
    inferenceRunner.init()
  }

  override val supervisorStrategy = OneForOneStrategy() {
    case _ : Exception => Escalate
  }

  def receive = {
    case InferenceManager.GroundFactorGraph(factorDescs, calibrationSettings,
      skipLearning, weightTable) =>
      val _sender = sender
      try {
        inferenceRunner.asInstanceOf[SQLInferenceRunner]
          .groundFactorGraph(variableSchema, factorDescs, calibrationSettings,
            skipLearning, weightTable, dbSettings)
        sender ! "OK"
      } catch {
        // If some exception is thrown, terminate DeepDive
        case e: Throwable =>
        sender ! Status.Failure(e)
        context.stop(self)
      }
    case InferenceManager.RunInference(factorDescs, holdoutFraction, holdoutQuery,
      samplerJavaArgs, samplerOptions, pipelineSettings, dbSettings) =>
      val _sender = sender
      val result = runInference(factorDescs, holdoutFraction, holdoutQuery,
        samplerJavaArgs, samplerOptions, pipelineSettings, dbSettings)
      result pipeTo _sender
  }

  def runInference(factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String],
    samplerJavaArgs: String, samplerOptions: String, pipelineSettings: PipelineSettings, dbSettings: DbSettings) = {

    val sampler = context.actorOf(samplerProps, "sampler")

    val samplingResult = sampler ? Sampler.Run(samplerJavaArgs, samplerOptions,
      factorGraphDumpFileWeights.getCanonicalPath, factorGraphDumpFileVariables.getCanonicalPath,
      factorGraphDumpFileFactors.getCanonicalPath, factorGraphDumpFileEdges.getCanonicalPath,
      factorGraphDumpFileMeta.getCanonicalPath, SamplingOutputDir.getCanonicalPath,
      pipelineSettings.baseDir, dbSettings.incrementalMode)
    // Kill the sampler after it's done :)
    sampler ! PoisonPill
    samplingResult.map { x =>
      inferenceRunner.writebackInferenceResult(
      variableSchema, SamplingOutputFile.getCanonicalPath,
      SamplingOutputFileWeights.getCanonicalPath, dbSettings)
    }
  }

}

object InferenceManager {

  /* An inference manager that uses postgres as its datastore */
  class PostgresInferenceManager(val taskManager: ActorRef, val variableSchema: Map[String, _ <: VariableDataType], val dbSettings: DbSettings)
    extends InferenceManager with PostgresInferenceRunnerComponent {
    lazy val inferenceRunner = new PostgresInferenceRunner(dbSettings)
  }

  /* An inference manager that uses postgres as its datastore */
  class MysqlInferenceManager(val taskManager: ActorRef, val variableSchema: Map[String, _ <: VariableDataType], val dbSettings: DbSettings)
    extends InferenceManager with MysqlInferenceRunnerComponent {
    lazy val inferenceRunner = new MysqlInferenceRunner(dbSettings)
  }

  def props(taskManager: ActorRef, variableSchema: Map[String, _ <: VariableDataType],
    dbSettings: DbSettings) = {
    dbSettings.driver match {
       case "org.postgresql.Driver" => Props(classOf[PostgresInferenceManager], taskManager, variableSchema, dbSettings)

       case "com.mysql.jdbc.Driver" => Props(classOf[MysqlInferenceManager], taskManager, variableSchema, dbSettings)
    }
  }


  // Messages
  // ==================================================

  // Executes a task to build part of the factor graph
  case class GroundFactorGraph(factorDescs: Seq[FactorDesc], calibrationSettings: CalibrationSettings,
    skipLearning: Boolean, weightTable: String)
  // Runs the sampler with the given arguments
  case class RunInference(factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String],
    samplerJavaArgs: String, samplerOptions: String, pipelineSettings: PipelineSettings, dbSettings: DbSettings)

}
