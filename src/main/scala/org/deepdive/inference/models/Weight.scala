package org.deepdive.inference

import anorm._

case class Weight(id: Long, value: Double, isFixed: Boolean) extends CSVFormattable {
   def toCSVRow = Array(id.toString, value.toString, isFixed.toString)
}
