package org.deepdive.inference
import org.deepdive.settings._
import org.deepdive.Context
import java.io.File

object InferenceNamespace {

  val deepdivePrefix = "dd_"
  def WeightsTable = "dd_graph_weights"
  def lastWeightsTable = "dd_graph_last_weights"
  def FactorsTable = "dd_graph_factors"
  def VariablesTable = "dd_graph_variables"
  def VariablesMapTable = "dd_graph_variables_map"
  def WeightResultTable = "dd_inference_result_weights"
  def VariablesHoldoutTable = "dd_graph_variables_holdout"
  def VariableResultTable = "dd_inference_result_variables"
  def MappedInferenceResultView = "dd_mapped_inference_result"
  def IdSequence = "dd_variable_sequence"
  def FactorMetaTable = "dd_graph_factormeta"
  def VariablesObservationTable = "dd_graph_variables_observation"
  def LearnedWeightsTable = "dd_inference_result_weights_mapping"
  def FeatureStatsSupportTable = "dd_feature_statistics_support"
  def FeatureStatsView = "dd_feature_statistics"

  // internal tables
  def getWeightTableName(tableName: String) = s"dd_weights_${tableName}"
  def getQueryTableName(tableName: String) = s"dd_query_${tableName}"
  def getFactorTableName(tableName: String) = s"dd_factors_${tableName}"
  def getCardinalityTableName(relation: String, column: String) = s"dd_${relation}_${column}_cardinality"
  def getVariableTypeTableName(relation: String) = s"dd_${relation}_vtype"
  def getCardinalityInFactorTableName(prefix: String, idx: Int) = s"dd_${prefix}_cardinality_${idx}"
  def getIncrementalTableName(table: String) = s"dd_delta_${table}"
  def getLastTableName(table: String) = s"dd_last_${table}"
  // given an incremental table, get the table name for its base table
  // incremental table names are like dd_delta_...
  def getBaseTableName(table: String) = table.replaceAll("dd_new_", "").replaceAll("dd_delta_", "")
  def getIncrementalMetaTableName() = s"dd_incremental_meta_data"

  // files
  def getVariableFileName(relation: String) = s"dd_variables_${relation}"
  def getFactorFileName(name: String) = s"dd_factors_${name}_out"
  def getWeightFileName = s"dd_weights"
  def getFactorMetaFileName = s"dd_factormeta"

  // variable data type id, it's an enum type used to communicate with the sampler
  def getVariableDataTypeId(variable: VariableDataType) : Int = {
    variable match {
      case BooleanType => 0
      case MultinomialType(x) => 1
      case RealNumberType => 2
    }
  }

  // factor funce type id, enum used to communicate with the sampler
  def getFactorFunctionTypeid(functionName: String) : Int = {
    functionName match {
      case "ImplyFactorFunction" => 0
      case "OrFactorFunction" => 1
      case "AndFactorFunction" => 2
      case "EqualFactorFunction" => 3
      case "IsTrueFactorFunction" =>  4
      case "MultinomialFactorFunction" => 5
      case "LinearFactorFunction" => 7
      case "RatioFactorFunction" => 8
      case "LogicalFactorFunction" => 9
      case "LRFactorFunction" => 10
    }
  }

  // converting format scripts
  val utilFolder = "util"
  val formatConvertingScriptName = "tobinary.py"
  val formatConvertingWorkerName = "format_converter"

  def getFormatConvertingScriptPath : String = {
    new File(s"${Context.deepdiveHome}/${utilFolder}/${formatConvertingScriptName}").getCanonicalPath()
  }
  def getFormatConvertingWorkerPath : String = {
    new File(s"${Context.deepdiveHome}/${utilFolder}/${formatConvertingWorkerName}").getCanonicalPath()
  }

  def getActiveScript : String = new File(s"${Context.deepdiveHome}/${utilFolder}/active.sh").getCanonicalPath()

}
