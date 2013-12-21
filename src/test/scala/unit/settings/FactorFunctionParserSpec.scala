package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.Context
import org.deepdive.settings._

class FactorFunctionParserSpec extends FunSpec {

  describe("The Factor function parser") {

    it("should parse empty imply expressions") {
      val expr = "words.is_present = Imply()"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_present", false, false), List())
      )
    }

    it("should parse imply expressions with multiple arguments") {
      val expr = "words.is_true = Imply(relation2.predicate, relation3.predicate)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_true", false), 
        List(
          FactorFunctionVariable("relation2", "predicate", false), 
          FactorFunctionVariable("relation3", "predicate", false))
      ))
    }

    it("should parse expressions with deep identifiers") {
      val expr = "words.is_true = Imply(relation2.r2.predicate)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_true", false), 
        List(FactorFunctionVariable("relation2.r2", "predicate", false))
      ))
    }

    it("should parse array factor functions") {
      val expr = "words.is_present = Imply(words.char[])"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_present", false), List(
          FactorFunctionVariable("words", "char", true)))
      )
    }

    it("should parse expression with negated variables") {
      val expr = "titles.is_present = Imply(!words.is_present)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("titles", "is_present", false), List(
          FactorFunctionVariable("words", "is_present", false, true)))
      )
    }


    it("should not parse malformed Imply expressions") {
      val expr = "id = Imply(a"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }

}