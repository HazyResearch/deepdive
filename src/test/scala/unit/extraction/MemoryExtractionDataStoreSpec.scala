package org.deepdive.test.unit

import org.deepdive.extraction.datastore._
import org.scalatest._
import spray.json._

class MemoryExtractionDataStoreSpec extends FunSpec with BeforeAndAfter
  with MemoryExtractionDataStoreComponent {


  describe("Writing results") {
    
    it("should work") {
      val testRow = JsObject(Map[String, JsValue](
        "id" -> JsNumber(1),
        "key" -> JsNumber(100),
        "some_text" -> JsString("I am sample text."),
        "some_boolean" -> JsBoolean(false),
        "some_double" -> JsNumber(13.37)
      ))
      dataStore.write(List(testRow), "testRelation")
      assert(dataStore.data.size == 1)
    }

  }

  describe("Reading as JSON") {
    val testRow = JsObject(Map[String, JsValue](
      "id" -> JsNumber(1),
      "key" -> JsNumber(100),
      "some_text" -> JsString("I am sample text."),
      "some_boolean" -> JsBoolean(false),
      "some_double" -> JsNumber(13.37)
    ))
    dataStore.write(List(testRow), "testRelation")
    val queryResult = dataStore.queryAsJson("testRelation")
    assert(queryResult.toList.head == testRow)

  }

  describe("Reading as Map") {
    val testRow = JsObject(Map[String, JsValue](
      "id" -> JsNumber(1),
      "key" -> JsNumber(100),
      "some_text" -> JsString("I am sample text."),
      "some_boolean" -> JsBoolean(false),
      "some_double" -> JsNumber(13.37)
    ))
    dataStore.write(List(testRow), "testRelation")
    val queryResult = dataStore.queryAsMap("testRelation")
    assert(queryResult.toList.head == Map[String, Any](
      "id" -> BigDecimal(1),
      "key" -> BigDecimal(100),
      "some_text" -> "I am sample text.",
      "some_boolean" -> false,
      "some_double" -> BigDecimal(13.37)
    ))
  }

}