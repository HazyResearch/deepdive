package org.deepdive.inference

// Variable Type: CQS (Discrete, Query, Gibbs Sampling) or CES (Discrete, Evidence, Gibbs Sampling)
object VariableType extends Enumeration {
  type VariableType = Value
  val CQS, CES = Value
}
import VariableType._

case class Variable(id: Integer, variableType: VariableType, lowerBound: Double, upperBound: Double, initialValue: Double)