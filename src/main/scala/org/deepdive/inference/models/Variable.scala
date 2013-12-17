package org.deepdive.inference

import anorm._
import org.deepdive.Logging
import org.deepdive.datastore.Utils.AnormSeq

// Variable Type: CQS (Discrete, Query, Gibbs Sampling) or CES (Discrete, Evidence, Gibbs Sampling)
object VariableDataType extends Enumeration with Logging {
  type VariableDataType = Value
  val `Boolean`, Discrete, Continuous = Value
}
import VariableDataType._

case class Variable(id: Long, dataType: VariableDataType, initialValue: Double, 
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

case class VariableMappingKey(relation: String, id: Long, column: String) {
  override val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(VariableMappingKey.this)
}

object VariableMappingKey {
  implicit def toAnormSeq(value: VariableMappingKey) : AnormSeq = {
    Seq(
      ("mapping_relation", toParameterValue(value.relation)), 
      ("mapping_id", toParameterValue(value.id)),
      ("mapping_column", toParameterValue(value.column))
    )
  }
}