package org.deepdive.inference
import org.deepdive.settings._

object InferenceTableNameSpace {
  val weightTablePrefix = "dd_weights_"
  val queryTablePrefix = "dd_query_"
  val factorTablePrefix = "dd_factors_"
  
  def getWeightTableName(tableName: String) = s"${weightTablePrefix}${tableName}"
  def getQueryTableName(tableName: String) = s"${queryTablePrefix}${tableName}"
  def getFactorTableName(tableName: String) = s"${factorTablePrefix}${tableName}"

  // variable data type id
  def getVariableDataTypeId(variable: VariableDataType) : Int = {
    variable match {
      case BooleanType => 0
      case MultinomialType(x) => 1
    }
  }

  // factor funce type id
  def getFactorFunctionTypeid(functionName: String) : Int = {
    functionName match {
      case "ImplyFactorFunction" => 0
      case "OrFactorFunction" => 1
      case "AndFactorFunction" => 2
      case "EqualFactorFunction" => 3
      case "IsTrueFactorFunction" =>  4
      case "MultinomialFactorFunction" => 5
    }
  }
}

