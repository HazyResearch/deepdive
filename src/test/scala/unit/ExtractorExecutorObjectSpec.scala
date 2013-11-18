package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.extraction.ExtractorExecutor
import org.deepdive.context.Relation
import spray.json._

import DefaultJsonProtocol._

class ExtractorExecutorObjectSpec extends FunSpec {

  describe("buildInsert") {

    it("should work for one row") {
      val relation = Relation("words", Map("id" -> "Integer", "length" -> "Integer", "text" -> "Text"), Nil)
      val row = JsArray(List(5.toJson,"Hello".toJson))
      assert(ExtractorExecutor.buildInsert(List(row), relation) ===
        "INSERT INTO words (length, text) VALUES (5, 'Hello');")
    }

    it ("should work for multiple rows") {
      val relation = Relation("words", Map("id" -> "Integer", "length" -> "Integer", "text" -> "Text"), Nil)
      val row1 = JsArray(List(5.toJson,"Hello".toJson))
      val row2 = JsArray(List(5.toJson,"James".toJson))
      val row3 = JsArray(List(1.toJson,"B".toJson))
      assert(ExtractorExecutor.buildInsert(List(row1, row2, row3), relation) ===
        "INSERT INTO words (length, text) VALUES (5, 'Hello'), (5, 'James'), (1, 'B');")
    }

  }

}