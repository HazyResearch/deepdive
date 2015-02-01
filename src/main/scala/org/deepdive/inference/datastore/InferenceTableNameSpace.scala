package org.deepdive.inference


object InferenceTableNameSpace {

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

  val deepdivePrefix = "dd_"
  val weightTablePrefix = s"${deepdivePrefix}weights_"
  val queryTablePrefix = s"${deepdivePrefix}query_"
  val factorTablePrefix = s"${deepdivePrefix}factors_"
  val variableFilePrefix = s"${deepdivePrefix}variables_"
  
  def getWeightTableName(tableName: String) = s"${weightTablePrefix}${tableName}"
  def getQueryTableName(tableName: String) = s"${queryTablePrefix}${tableName}"
  def getFactorTableName(tableName: String) = s"${factorTablePrefix}${tableName}"
  def getCardinalityTableName(relation: String, column: String) = s"${relation}_${column}_cardinality"
  def getVariableFileName(fileName: String) = s"${variableFilePrefix}${relation}"
}

