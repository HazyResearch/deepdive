package org.deepdive.inference

case class Weight(id: Long, value: Double, isFixed: Boolean, description: String) extends CSVFormattable {
   def toCSVRow = Array(id.toString, value.toString, isFixed.toString, description)
}
