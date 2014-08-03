package org.deepdive.inference

case class Weight(id: Long, value: Double, isFixed: Boolean, description: String, weightLength:Long) extends CSVFormattable {
   def toCSVRow = Array(id.toString, value.toString, isFixed.toString, description, weightLength.toString)
}
