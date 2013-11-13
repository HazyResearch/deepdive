package org.deepdive

sealed trait FactorFunction {
  def variables : Seq[String]
}
case class ImplyFactorFunction(variables: Seq[String]) extends FactorFunction