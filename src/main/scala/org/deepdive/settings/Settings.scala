package org.deepdive.settings

import com.typesafe.config._
import org.deepdive.helpers.Helpers
import scala.collection.JavaConversions._
import scala.util.Try


case class Settings(
  schemaSettings : SchemaSettings = SchemaSettings(),
  extractionSettings: ExtractionSettings = ExtractionSettings(),
  inferenceSettings: InferenceSettings = InferenceSettings(),
  calibrationSettings: CalibrationSettings = CalibrationSettings(),
  samplerSettings: SamplerSettings = SamplerSettings(),
  pipelineSettings: PipelineSettings = PipelineSettings(),
  dbSettings: DbSettings = DbSettings(),
  config: Config // TODO the sanitized Config we'll eventually migrate most of the things to
) {
  // handy ways to update config
  def updatedConfig(config: Config): Settings = {
    copy(config = config)
  }
  def updatedConfig(path: String, config: Config): Settings = {
    copy(config = config.withValue(path, config.root))
  }
}

/* Calibration Settings */
case class CalibrationSettings(
  holdoutFraction: Double = 0.0,
  holdoutQuery: Option[String] = None,
  observationQuery: Option[String] = None
)

object IncrementalMode extends Enumeration {
  type IncrementalMode = Value
  // INCREMENTAL => Generate incremental application.conf
  // MERGE => Merge new generated data into original table
  val ORIGINAL, INCREMENTAL, MATERIALIZATION = Value
}

/* Database connection specifie in the settings */
case class DbSettings(
  // TODO switch to Option type instead of using null
  driver: String = null,
  url: String = null,
  user: String = null,
  password: String = null,
  dbname: String = null,
  host: String = null,
  port: String = null,
  gphost: String = null,
  gppath: String = null,
  gpport: String = null,
  gpload: Boolean = false,
  incrementalMode: IncrementalMode.IncrementalMode = IncrementalMode.ORIGINAL,
  keyMap: Map[String, List[String]] = null
)


/* Extraction Settings */
case class ExtractionSettings(
  extractors: List[Extractor] = List.empty,
  parallelism: Int = 1
)

/* Extractor specified in the settings */
case class Extractor(
  name: String,
  style: String,

  outputRelation: String = "", // tsv, json, plpy, piggy
  inputQuery: InputQuery = null, // tsv, json, plpy, piggy
  udfDir: String = null, // tsv, json, plpy, piggy
  udf: String = "", // tsv, json, plpy, piggy

  parallelism: Int = 1,
  inputBatchSize: Int = 10000,
  outputBatchSize: Int = 50000,
  dependencies: Set[String] = Set.empty,
  beforeScript: Option[String] = None,
  afterScript: Option[String] = None,


  sqlQuery: String = "", // sql_extractor

  cmd: Option[String] = None, // cmd_extractor

  loader: String = "",
  loaderConfig: LoaderConfig = null
)

/* A Factor specified in the settings */
case class FactorDesc(
  name: String,
  inputQuery: String,
  func: FactorFunction,
  weight: FactorWeight,
  weightPrefix: String
)

/* Factor Weight for a factor specified in the settings*/
sealed trait FactorWeight {
  def variables : List[String]
}

/* A factor weight with a known value */
case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
}

/* A factor weight with an unknown value */
case class UnknownFactorWeight(variables: List[String]) extends FactorWeight

import scala.language.implicitConversions

/* A generic Factor Functions */
sealed trait FactorFunction {
  def variables : Seq[FactorFunctionVariable]
  /* Data type can be one of: Boolean, Discrete, Continuous */
  def variableDataType : String
  /* The relations used in this factor function */
  def relations = variables.map(_.relation).toSet
}

abstract class BooleanFactorFunction extends FactorFunction {
  override def variableDataType: String = "Boolean"
}
abstract class DiscreteFactorFunction extends FactorFunction {
  override def variableDataType: String = "Discrete"
}

/* A factor function of fom A and B and C ... -> Z */
case class ImplyFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing A or B or C ... */
case class OrFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing A and B and C ... */
case class AndFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing A == B. Restricted to two variables. */
case class EqualFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing A == True. Restricted to one variable. */
case class IsTrueFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing XOR(A,B,C,...) */
case class XorFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing and between all combinations of values for multinomial variables */
case class MultinomialFactorFunction(variables: Seq[FactorFunctionVariable]) extends DiscreteFactorFunction

/* A factor function describing linear semantics, (A -> Z) + (B -> Z) + (C -> Z). */
case class LinearFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing ratio semantics, log(1 + (A -> Z) + (B -> Z) + (C -> Z)). */
case class RatioFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A factor function describing logical semantics, (A -> Z) or (B -> Z) or (C -> Z)*/
case class LogicalFactorFunction(variables: Seq[FactorFunctionVariable]) extends BooleanFactorFunction

/* A variable used in a Factor function */
case class FactorFunctionVariable(relation: String, field: String, isArray: Boolean = false,
  isNegated: Boolean = false, predicate: Option[Long] = None) {
  override def toString = s"${relation}.${field}"
  def headRelation = relation.split('.').headOption.getOrElse(relation)
  def key = s"${headRelation}.${field}"
}

/* Companion object for factor function variables */
object FactorFunctionVariable {
  implicit def stringToFactorFunctionVariable(str: String) : FactorFunctionVariable = {
    FactorFunctionParser.parse(FactorFunctionParser.factorVariable, str).get
  }
}

case class InferenceSettings(
  factors: List[FactorDesc] = Nil,
  insertBatchSize: Option[Int] = None,
  skipLearning: Boolean = false,
  weightTable: String = "",
  parallelGrounding: Boolean = false
)

import scala.language.implicitConversions

sealed trait InputQuery

case class CSVInputQuery(filename: String, seperator: Char) extends InputQuery
case class DatastoreInputQuery(query: String) extends InputQuery

object InputQuery {
  implicit def stringToInputquery(str: String) : InputQuery = {
    InputQueryParser.parse(InputQueryParser.inputQueryExpr, str).get
  }
}

/* Extractor specified in the settings */
case class LoaderConfig (
  connection: String,
  schemaFile: String,
  threads: Int,
  parallelTransactions: Int
)

/* A DeepDive pipeline that specifies which extractors and factor tasks should be executed */
case class Pipeline(id: String, tasks: Set[String])

/* User settings pipelines */
case class PipelineSettings(
  activePipelineName: Option[String] = None,
  pipelines: List[Pipeline] = List.empty,
  relearnFrom: String = null, // TODO Option
  baseDir: Option[String] = None
) {
  def activePipeline: Option[Pipeline] = activePipelineName flatMap { name =>
    pipelines find (_.id == name)
  }
}

/* Calibration Settings */
case class SamplerSettings(
  samplerCmd: String = null,
  samplerArgs: String = ""
)

/* Schema Settings */
case class SchemaSettings(
  variables: Map[String, _ <: VariableDataType] = Map.empty,
  setupFile: Option[String] = None
)

sealed trait VariableDataType {
  def cardinality: Int
}
case object BooleanType extends VariableDataType {
  def cardinality = 2
  override def toString() = "Boolean"
}
case class MultinomialType(cardinality: Int) extends VariableDataType {
  override def toString() = s"Multinomial(${cardinality})"
}
