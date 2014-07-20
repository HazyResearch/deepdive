package org.deepdive.settings

/* A Factor specified in the settings */
case class FactorDesc(name: String, inputQuery: String, func: FactorFunction, 
  weight: FactorWeight, weightPrefix: String)

/* Factor Weight for a factor specified in the settings*/
sealed trait FactorWeight {
  def variables : List[String]
  def vectorLength : Integer
}

/* A factor weight with a known value */ 
case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
  def vectorLength = 0
}

case class KnownFactorWeightVector(value: Double, vectorLength : Integer) extends FactorWeight {
  def variables = Nil
}

/* A factor weight with an unknown value */
case class UnknownFactorWeight(variables: List[String]) extends FactorWeight{
  def vectorLength = 0
}

case class UnknownFactorWeightVector(variables: List[String], vectorLength : Integer) extends FactorWeight

