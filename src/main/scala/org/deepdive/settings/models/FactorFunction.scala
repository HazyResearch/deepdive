package org.deepdive.settings

/* A generic Factor Functions */
sealed trait FactorFunction {
  def variables : Seq[FactorFunctionVariable]
  /* Data type can be one of: Boolean, Discrete, Continuous */
  def variableDataType : String = "Boolean"
}

/* A factor function of fom A and B and C ... -> Z */
case class ImplyFactorFunction(head: FactorFunctionVariable, 
  body: Seq[FactorFunctionVariable]) extends FactorFunction {
  def variables = Seq(head) ++ body 
  override def variableDataType = "Boolean"
}

/* A factor function of fom A and B and C ... -> Z */
case class DummyFactorFunction(val variables: Seq[FactorFunctionVariable]) extends FactorFunction {
  override def variableDataType = "Discrete"
}


/* A variable used in a Factor function */
case class FactorFunctionVariable(relation: String, field: String, isArray: Boolean) {
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