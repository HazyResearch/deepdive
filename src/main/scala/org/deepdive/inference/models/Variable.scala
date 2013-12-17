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
  isEvidence: Boolean, isQuery: Boolean) extends CSVFormattable {
  def toCSVRow = Array(id.toString, dataType.toString, initialValue.toString, isEvidence.toString, 
      isQuery.toString)
}

case class VariableMappingKey(relation: String, id: Long, column: String) extends CSVFormattable {
  override val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(VariableMappingKey.this)
  def toCSVRow = Array(relation.toString, id.toString, column.toString)
}
