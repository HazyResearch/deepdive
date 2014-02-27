package org.deepdive.settings

/* Schema Settings */
case class SchemaSettings(variables: Map[String, _ <: VariableDataType])

sealed trait VariableDataType {
  def cardinality: Option[Long]
}
case object BooleanType extends VariableDataType {
  def cardinality = None
}
case class MultinomialType(numCategories: Int) extends VariableDataType {
  def cardinality = Some(numCategories)
}

