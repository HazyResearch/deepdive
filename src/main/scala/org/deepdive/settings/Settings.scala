package org.deepdive.settings

import com.typesafe.config._
import scala.collection.JavaConversions._
import scala.util.Try


object Settings {
  def loadFromConfig(config: Config) = SettingsParser.loadFromConfig(config)
}

case class Settings(schemaSettings : SchemaSettings,
  extractionSettings: ExtractionSettings,
  inferenceSettings: InferenceSettings,
  calibrationSettings: CalibrationSettings,
  samplerSettings: SamplerSettings,
  pipelineSettings: PipelineSettings,
  dbSettings: DbSettings)


/* Calibration Settings */
case class CalibrationSettings(holdoutFraction: Double, holdoutQuery: Option[String], observationQuery: Option[String])

/* Database connection specifie in the settings */
case class Connection(url: String, user: String, password: String)

object IncrementalMode extends Enumeration {
  type IncrementalMode = Value
  // INCREMENTAL => Generate incremental application.conf
  // MERGE => Merge new generated data into original table
  val ORIGINAL, INCREMENTAL, MATERIALIZATION = Value
}

/* Database connection specifie in the settings */
case class DbSettings(driver: String, url: String, user: String, password: String,
  dbname: String, host: String, port: String, gphost: String, gppath: String,
  gpport: String, gpload: Boolean, incrementalMode: IncrementalMode.IncrementalMode,
  keyMap: Map[String, List[String]] = null)


/* Extraction Settings */
case class ExtractionSettings(extractors: List[Extractor], parallelism: Int)

/* Extractor specified in the settings */
case class Extractor(
  name: String,
  style: String,
  outputRelation: String,
  inputQuery: InputQuery,
  udfDir: String,
  udf: String,
  parallelism: Int,
  inputBatchSize: Int,
  outputBatchSize: Int,
  dependencies: Set[String],
  beforeScript: Option[String] = None,
  afterScript: Option[String] = None,
  sqlQuery: String = "",
  cmd: Option[String] = None,
  loader: String = "",
  loaderConfig: LoaderConfig = null
  )

/* A Factor specified in the settings */
case class FactorDesc(name: String, inputQuery: String, func: FactorFunction,
  weight: FactorWeight, weightPrefix: String)

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
  def variableDataType : String = "Boolean"
  /* The relations used in this factor function */
  def relations = variables.map(_.relation).toSet
}

/* A factor function of fom A and B and C ... -> Z */
case class ImplyFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing A or B or C ... */
case class OrFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing A and B and C ... */
case class AndFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing A == B. Restricted to two variables. */
case class EqualFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing A == True. Restricted to one variable. */
case class IsTrueFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing XOR(A,B,C,...) */
case class XorFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing and between all combinations of values for multinomial variables */
case class MultinomialFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Discrete"
}

/* A factor function describing linear semantics, (A -> Z) + (B -> Z) + (C -> Z). */
case class LinearFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing ratio semantics, log(1 + (A -> Z) + (B -> Z) + (C -> Z)). */
case class RatioFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

/* A factor function describing logical semantics, (A -> Z) or (B -> Z) or (C -> Z)*/
case class LogicalFactorFunction(variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Boolean"
}

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

case class InferenceSettings(factors: List[FactorDesc], insertBatchSize: Option[Int],
  skipLearning: Boolean = false, weightTable: String = "", parallelGrounding: Boolean = false)

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
case class PipelineSettings(activePipelineName: Option[String], pipelines: List[Pipeline],
  relearnFrom: String, baseDir: Option[String]) {
  def activePipeline : Option[Pipeline] = activePipelineName.flatMap { name =>
    pipelines.find(_.id == name)
  }
}

/* Calibration Settings */
case class SamplerSettings(samplerCmd: String, samplerArgs: String)

/* Schema Settings */
case class SchemaSettings(variables: Map[String, _ <: VariableDataType], setupFile: Option[String])

// sealed trait VariableDataType {
//   def cardinality: Option[Long]
// }
// case object BooleanType extends VariableDataType {
//   def cardinality = None
//   override def toString() = "Boolean"
// }
// case class MultinomialType(numCategories: Int) extends VariableDataType {
//   def cardinality = Some(numCategories)
//   override def toString() = "Multinomial"
// }

sealed trait VariableDataType {
  def cardinality: Long
}
case object BooleanType extends VariableDataType {
  def cardinality = 2
  override def toString() = "Boolean"
}
case class MultinomialType(numCategories: Int) extends VariableDataType {
  def cardinality = numCategories
  override def toString() = "Multinomial"
}
