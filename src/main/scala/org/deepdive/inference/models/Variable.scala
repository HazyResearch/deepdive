package org.deepdive.inference

import anorm._
import org.deepdive.Logging

// Variable Type: CQS (Discrete, Query, Gibbs Sampling) or CES (Discrete, Evidence, Gibbs Sampling)
object VariableDataType extends Enumeration with Logging {
  type VariableDataType = Value
  val `Boolean`, Discrete, Continuous = Value
}
import VariableDataType._

case class Variable(id: Long, dataType: VariableDataType, initialValue: Double, 
  isEvidence: Boolean, isQuery: Boolean, mapping_relation: String, mapping_column: String) extends CSVFormattable {
  
  def toCSVRow = Array(id.toString, dataType.toString, initialValue.toString, isEvidence.toString, 
      isQuery.toString, mapping_relation, mapping_column)
}