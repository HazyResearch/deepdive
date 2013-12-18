package org.deepdive.inference

trait CSVFormattable {
  def toCSVRow : Array[String]
}

object CSVFormattable {

  implicit def stringArrayToCSVFormattable(x: Array[String]) : CSVFormattable = {
    new CSVFormattable(){
      def toCSVRow = x
    }
  }

  implicit def combineCSVIterators[A <: CSVFormattable, B <: CSVFormattable](x: Iterator[(A, B)]) : 
    Iterator[CSVFormattable] = {
    x.map { case(a, b) =>
      a.toCSVRow ++ b.toCSVRow
    }
  }
}