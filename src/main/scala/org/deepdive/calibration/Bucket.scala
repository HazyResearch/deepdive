package org.deepdive.calibration

import scala.language.implicitConversions

/* Helper class for a probability bucket used for histogram plotting. */
case class Bucket(from: Double, to: Double)

/* Companion object for Bucket class */
object Bucket {

  /* Converts a Tuple2[Double] to a Bucket instance */
  implicit def tupleToBucket(x: (Double, Double)) : Bucket = Bucket(x._1, x._2)

  /* Return 10 Buckets: (0.0, 0.1), (0.1, 0.2), ... */
  def ten : List[Bucket] = {
    val a = 0.0.to(0.9, 0.1)
    val b = 0.1.to(1.0, 0.1)
    a.zip(b).map(tupleToBucket).toList
  }

}