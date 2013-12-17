package org.deepdive.inference

trait CSVFormattable {
  def toCSVRow : Array[String]
}