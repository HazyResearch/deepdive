package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.settings._

class FactorWeightParserSpec extends FunSpec {

  describe("The FactorWeight parser") {

    it("should parse a constant factor weight") {
      val expr = "5"
      val result = FactorWeightParser.parse(FactorWeightParser.factorWeight, expr)
      assert(result.successful)
      assert(result.get == KnownFactorWeight(5.0))
    }

    it("should parse a floating point factor weight") {
      val expr = "0.5"
      val result = FactorWeightParser.parse(FactorWeightParser.factorWeight, expr)
      assert(result.successful)
      assert(result.get == KnownFactorWeight(.5))
    }

    it("should parse an unknown weight without variables") {
      val expr = "?"
      val result = FactorWeightParser.parse(FactorWeightParser.factorWeight, expr)
      assert(result.successful)
      assert(result.get == UnknownFactorWeight(List()))
    }

    it("should parse an unknown weight with variables") {
      val expr = "?(relation1.is_present, relation2.is_present)"
      val result = FactorWeightParser.parse(FactorWeightParser.factorWeight, expr)
      assert(result.successful)
      assert(result.get == UnknownFactorWeight(List("relation1.is_present", "relation2.is_present")))
    }

    it("should parse a variable with .") {
      val expr = "?(.someField.is_present)"
      val result = FactorWeightParser.parse(FactorWeightParser.factorWeight, expr)
      assert(result.successful)
      assert(result.get == UnknownFactorWeight(List(".someField.is_present")))
    }

  }

}