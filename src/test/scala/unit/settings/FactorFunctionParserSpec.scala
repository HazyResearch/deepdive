package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.Context
import org.deepdive.settings._

class FactorFunctionParserSpec extends FunSpec {

  describe("Parsing Imply expressions") {

    it("should parse imply expressions with one variable") {
      val expr = "Imply(words.is_present)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_present", false, false), List())
      )
    }


    it("should parse imply expressions with multiple variables") {
      val expr = "Imply(relation2.predicate, relation3.predicate, words.is_true)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(
        FactorFunctionVariable("words", "is_true", false), 
        List(
          FactorFunctionVariable("relation2", "predicate", false), 
          FactorFunctionVariable("relation3", "predicate", false))
      ))
    }

    it("should not parse malformed Imply expressions") {
      val expr = "Imply(a"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }

  describe("Parsing AND factor functions") {

    it("should work") {
      val expr = "And(a.b)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get === AndFactorFunction(List(FactorFunctionVariable("a", "b", false, false))))
    }

  }

  describe("Parsing OR factor functions") {

    it("should work") {
      val expr = "Or(a.b, c.d)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get === OrFactorFunction(List(
        FactorFunctionVariable("a", "b", false, false),
        FactorFunctionVariable("c", "d", false, false))))
    }

  }

  describe("Parsing EQUAL factor functions") {

    it("should work for two variables") {
      val expr = "Equal(a.b, c.d)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get === EqualFactorFunction(List(
        FactorFunctionVariable("a", "b", false, false),
        FactorFunctionVariable("c", "d", false, false))))
    }

    it("should fail for more than two variables") {
      val expr = "Equal(a.b, c.d, e.f)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

    it("should fail for less than two variables") {
      val expr = "Equal(a.b)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }

  describe("Parsing IsTrue factor functions") {

    it("should work for one variable") {
      val expr = "IsTrue(a.b)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get === IsTrueFactorFunction(List(
        FactorFunctionVariable("a", "b", false, false))))
    }

    it("should fail for more than one variable") {
      val expr = "IsTrue(a.b, c.d)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }




  describe("Parsing variables") {
    it("should parse expressions with deep identifiers") {
      val expr = "relation2.r2.predicate"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorVariable, expr)
      assert(result.successful)
      assert(result.get == FactorFunctionVariable("relation2.r2", "predicate", false))
    }

    it("should parse array factor functions") {
      val expr = "words.char[]"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorVariable, expr)
      assert(result.successful)
      assert(result.get == FactorFunctionVariable("words", "char", true))
    }

    it("should parse expression with negated variables") {
      val expr = "!words.is_present"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorVariable, expr)
      assert(result.successful)
      assert(result.get === FactorFunctionVariable("words", "is_present", false, true))
    }
  }

}