package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.settings._

class DataTypeParserSpec extends FunSpec {

  describe("The DataType Parser parser") {

    it("should parse Boolean variables") {
      val expr = "Boolean"
      val result = DataTypeParser.parse(DataTypeParser.dataType, expr)
      assert(result.successful)
      assert(result.get == BooleanType)
    }

    it("should parse categorical variables") {
      val expr = "Categorical(5)"
      val result = DataTypeParser.parse(DataTypeParser.dataType, expr)
      assert(result.successful)
      assert(result.get == MultinomialType(5))
    }

    it("should fail if not Boolean or categorical variables"){
      val expr = "NotBooleanNorCategorical"
      val result = DataTypeParser.parse(DataTypeParser.dataType, expr)
      assert(!result.successful)
    }
  }
}