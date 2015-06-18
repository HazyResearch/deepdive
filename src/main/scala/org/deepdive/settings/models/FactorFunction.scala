package org.deepdive.settings

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
