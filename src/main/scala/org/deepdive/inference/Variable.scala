package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Utils.AnormSeq

// Variable Type: CQS (Discrete, Query, Gibbs Sampling) or CES (Discrete, Evidence, Gibbs Sampling)
object VariableType extends Enumeration {
  type VariableType = Value
  val CQS, CES = Value
}
import VariableType._

case class Variable(id: Integer, variableType: VariableType, lowerBound: Double, upperBound: Double, initialValue: Double)

object Variable {
  implicit def toAnormSeq(value: Variable) : AnormSeq = {
    Seq(
      ("id", toParameterValue(value.id)), 
      ("variable_type", toParameterValue(value.variableType.toString)),
      ("lower_bound", toParameterValue(value.lowerBound)),
      ("upper_bound", toParameterValue(value.upperBound)),
      ("initial_value", toParameterValue(value.initialValue))
    )
  }
}
