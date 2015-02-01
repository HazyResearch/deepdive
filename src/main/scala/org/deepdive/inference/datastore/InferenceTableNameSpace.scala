package org.deepdive.inference


object InferenceTableNameSpace {
  val weightTablePrefix = "dd_weights_"
  val queryTablePrefix = "dd_query_"
  val factorTablePrefix = "dd_factors_"
  
  def getWeightTableName(tableName: String) = "${weightTablePrefix}${tableName}"
  def getQueryTableName(tableName: String) = "${queryTablePrefix}${tableName}"
  def getFactorTableName(tableName: String) = "${factorTablePrefix}${tableName}"

}

