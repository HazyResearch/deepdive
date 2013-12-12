package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.context._
import org.deepdive.settings._

class FactorFunctionParserSpec extends FunSpec {

  describe("The Factor function parser") {

    it("should parse empty imply expressions") {
      val expr = "words.is_present = Imply()"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_present"), List())
      )
    }

    it("should parse imply expressions with multiple arguments") {
      val expr = "words.is_true = Imply(relation2.predicate, relation3.predicate)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_true"), 
        List(
          FactorFunctionVariable("relation2", "predicate"), 
          FactorFunctionVariable("relation3", "predicate"))
      ))
    }

    it("should parse expressions with deep identifiers") {
      val expr = "words.is_true = Imply(relation2.r2.predicate)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_true"), 
        List(FactorFunctionVariable("relation2.r2", "predicate"))
      ))
    }

    it("should not parse malformed Imply expressions") {
      val expr = "id = Imply(a"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }

}