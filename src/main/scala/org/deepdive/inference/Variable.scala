package org.deepdive.inference

import anorm._
import org.deepdive.Logging
import org.deepdive.datastore.Utils.AnormSeq

// Variable Type: CQS (Discrete, Query, Gibbs Sampling) or CES (Discrete, Evidence, Gibbs Sampling)
object VariableDataType extends Enumeration with Logging {
  type VariableDataType = Value
  val `Boolean`, Discrete, Continuous = Value

  def forAttributeType(attributeType: String) = attributeType match {
    case "Boolean" => `Boolean`
    case "Long" => Discrete
    case "Integer" => Discrete
    case "Decimal" => Continuous
    case "Float" => Continuous
    case x =>
      log.warning(s"Unknown variable_field_type=$x")
      `Boolean`
  }
}
import VariableDataType._

case class Variable(id: Integer, dataType: VariableDataType, initialValue: Double, 
  isEvidence: Boolean, isQuery: Boolean)

object Variable {
  implicit def toAnormSeq(value: Variable) : AnormSeq = {
    Seq(
      ("id", toParameterValue(value.id)), 
      ("data_type", toParameterValue(value.dataType.toString)),
      ("initial_value", toParameterValue(value.initialValue)),
      ("is_evidence", toParameterValue(value.isEvidence)),
      ("is_query", toParameterValue(value.isQuery))
    )
  }
}
