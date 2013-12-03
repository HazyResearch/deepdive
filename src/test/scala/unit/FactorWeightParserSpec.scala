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

    it("should parse an unknown weight without variables") {
      val expr = "?"
      val result = FactorWeightParser.parse(FactorWeightParser.factorWeight, expr)
      assert(result.successful)
      assert(result.get == UnknownFactorWeight(List()))
    }

    it("should parse an unknown weight with variables") {
      val expr = "?(entity_text, uppercase)"
      val result = FactorWeightParser.parse(FactorWeightParser.factorWeight, expr)
      assert(result.successful)
      assert(result.get == UnknownFactorWeight(List("entity_text", "uppercase")))
    }

  }

}