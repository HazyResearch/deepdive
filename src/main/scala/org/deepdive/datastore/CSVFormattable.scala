package org.deepdive.inference

/* Describes how an object can be represented as a CSV-formatted row */
trait CSVFormattable {
  def toCSVRow : Array[String]
}