package org.deepdive.inference

import org.deepdive.Logging
import org.deepdive.settings.VariableDataType


case class Variable(id: Long, dataType: VariableDataType, initialValue: Double, 
  isEvidence: Boolean, isQuery: Boolean, mappingRelation: String, 
  mappingColumn: String, mappingId: Long) extends CSVFormattable {
  
  def toCSVRow = Array(id.toString, dataType.toString, initialValue.toString, 
    isEvidence.toString, isQuery.toString, mappingRelation, mappingColumn, mappingId.toString)
}