package org.deepdive.calibration

case class Bucket(from: Double, to: Double)

object Bucket {
  implicit def tupleToBucket(x: (Double, Double)) : Bucket = Bucket(x._1, x._2)

  def ten : List[Bucket] = {
    val a = 0.0.to(0.9, 0.1)
    val b = 0.1.to(1.0, 0.1)
    a.zip(b).map(tupleToBucket).toList
  }

}