package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.context._
import org.deepdive.settings._

class FactorFunctionParserSpec extends FunSpec {

  describe("The Factor function parser") {

    it("should parse empty imply expressions") {
      val expr = "id = Imply()"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction("id", List()))
    }

    it("should parse imply expressions with multiple arguments") {
      val expr = "id = Imply(a,b)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction("id", List("a","b")))
    }

    it("should not parse malformed Imply expressions") {
      val expr = "id = Imply(a"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }

}