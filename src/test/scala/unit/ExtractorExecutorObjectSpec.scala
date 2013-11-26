package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.extraction.ExtractorExecutor
import org.deepdive.context.Relation
import spray.json._

import DefaultJsonProtocol._

class ExtractorExecutorObjectSpec extends FunSpec {

  describe("buildInsert") {

    it("should work") {
      val relation = Relation("words", Map("id" -> "Integer", "length" -> "Integer", "text" -> "Text"), Nil, None)
      val row = JsArray(List(5.toJson,"Hello".toJson))
      assert(ExtractorExecutor.buildInsert(relation) ===
        "INSERT INTO words (length, text) VALUES ({length}, {text});")
    }

  }

}