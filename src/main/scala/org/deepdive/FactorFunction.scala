package org.deepdive

trait FactorFunction
case class ImplyFactorFunction(variables: Seq[String]) extends FactorFunction