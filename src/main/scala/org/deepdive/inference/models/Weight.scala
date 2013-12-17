package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Utils.AnormSeq

case class Weight(id: Long, value: Double, isFixed: Boolean) extends CSVFormattable {
   def toCSVRow = Array(id.toString, value.toString, isFixed.toString)
}
