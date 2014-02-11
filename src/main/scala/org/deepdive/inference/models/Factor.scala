package org.deepdive.inference

case class Factor(id: Long, factorFunction: String, weightId: Long, 
  variables: List[FactorVariable]) extends CSVFormattable {
  def toCSVRow = Array(id.toString, weightId.toString, factorFunction.toString)
}

case class FactorVariable(factorId: Long, position: Long, positive: Boolean, 
  variableId: Long) extends  CSVFormattable {
  def toCSVRow =  Array(factorId.toString, variableId.toString, position.toString, positive.toString)
}
