package org.deepdive.settings

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
case object RealNumberType extends VariableDataType {
  def cardinality = 2
  override def toString() = "RealNumber"
}

case class RealArrayType(nNumber: Int) extends VariableDataType {
  def cardinality = nNumber
  def nele = nNumber
  override def toString() = "RealNumberArray"
}