package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.context._
import org.deepdive.settings._

class FactorFunctionParserSpec extends FunSpec {

  describe("The Factor function parser") {

    it("should parse empty imply expressions") {
      val expr = "is_true = Imply()"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable(None, "is_true"), List())
      )
    }

    it("should parse imply expressions with multiple arguments") {
      val expr = "is_true = Imply(relation2_id->a, relation3_id->b)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable(None, "is_true"), 
        List(
          FactorFunctionVariable(Option("relation2_id"), "a"), 
          FactorFunctionVariable(Option("relation3_id"), "b"))
      ))
    }

    it("should not parse malformed Imply expressions") {
      val expr = "id = Imply(a"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }

}