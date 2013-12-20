package org.deepdive.settings

/* A Factor specified in the settings */
case class FactorDesc(name: String, inputQuery: String, func: FactorFunction, 
  weight: FactorWeight, weightPrefix: String)

/* Factor Weight for a factor specified in the settings*/
sealed trait FactorWeight {
  def variables : List[String]
}

/* A factor weight with a known value */ 
case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
}

/* A factor weight with an unknown value */
case class UnknownFactorWeight(variables: List[String]) extends FactorWeight