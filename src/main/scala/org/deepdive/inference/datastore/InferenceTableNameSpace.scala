package org.deepdive.inference


object InferenceTableNameSpace {
  val weightTablePrefix = "dd_weights_"
  val queryTablePrefix = "dd_query_"
  val factorTablePrefix = "dd_factors_"
  
  def getWeightTableName(tableName: String) = s"${weightTablePrefix}${tableName}"
  def getQueryTableName(tableName: String) = s"${queryTablePrefix}${tableName}"
  def getFactorTableName(tableName: String) = s"${factorTablePrefix}${tableName}"

}

