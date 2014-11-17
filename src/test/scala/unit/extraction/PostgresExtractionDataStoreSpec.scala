package org.deepdive.test.unit

import anorm._
import org.deepdive.datastore._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.test._
import org.scalatest._
import scala.io.Source
import play.api.libs.json._
import java.io.StringWriter

class PostgresExtractionDataStoreSpec extends FunSpec with BeforeAndAfter
  with PostgresExtractionDataStoreComponent {

  lazy implicit val connection = PostgresDataStore.borrowConnection()

  before {
    JdbcDataStore.init()
    dataStore.init()
    SQL("drop schema if exists public cascade; create schema public;").execute()
    SQL("""create table datatype_test(id bigserial primary key, key integer, some_text text, 
      some_boolean boolean, some_double double precision, some_null boolean, 
      some_array text[]);""").execute()
  }

  after {
    JdbcDataStore.close()
  }

  describe("Querying as a Map") {

    def insertSampleData() = {
      SQL("""insert into datatype_test(key) 
        VALUES (1), (2), (3), (4)""").execute()
    }

    it("should work for simple attributes") {
      insertSampleData()
      val result = dataStore.queryAsMap("SELECT key from datatype_test order by key asc;")(_.toList)
      assert(result.head === Map("key" -> 1))
      assert(result.size === 4)
    }

    it("should work for aliased attributes") {
      insertSampleData()
      val result = dataStore.queryAsMap("SELECT key AS \"d1.key2\" from datatype_test order by \"d1.key2\" asc;")(_.toList)
      assert(result.head === Map("d1.key2" -> 1))
    }

    it("should work for aggregated attributes") {
      insertSampleData()
      val result = dataStore.queryAsMap("""SELECT COUNT(*) AS num 
        from datatype_test GROUP BY key""")(_.toList)
      assert(result.head === Map("num" -> 1))
    }

    it("should work with empty tables") {
      val result = dataStore.queryAsMap("SELECT * from datatype_test;")(_.toList)
      assert(result == Nil)
    }

    it("should throw an exception for invalid queries") {
      intercept[org.postgresql.util.PSQLException] {
        val result = dataStore.queryAsMap("SELECT *a from datatype_test;")(_.toList)
        assert(result == Nil)
      }
    }

  }

  describe("Querying as JSON") {

    def insertSampleData() = {
      SQL("""insert into datatype_test(key) 
        VALUES (1), (2), (3), (4)""").execute()
    }

    it("should work with empty tables") {
      val result = dataStore.queryAsJson("SELECT * from datatype_test;")(_.toList)
      assert(result == Nil)
    }

  }

  describe("Serializing to JSON") {  

    def insertSampleRow() : Unit = {
      SQL("""insert into datatype_test(key, some_text, some_boolean, some_double, some_array) 
        VALUES 
          (1, 'Hello', true, 1.0, '{"A","B"}'), 
          (1, 'Ce', false, 2.3, '{"C","D"}')""").execute()
    }

    it("should work with aggregate data types") {
      insertSampleRow()
      val result = dataStore.queryAsJson(
        """SELECT key, array_agg(some_text ORDER BY some_text) AS "datatype_test.texts"
        FROM datatype_test GROUP BY key"""
      )(_.toList)
      assert(result.head.asInstanceOf[JsObject].value == Map[String, JsValue](
        "key" -> JsNumber(1),
        "datatype_test.texts" -> JsArray(Seq(JsString("Ce"), JsString("Hello")))
      ))
    }

    it("should work with simple data types") {
      insertSampleRow()
      val result = dataStore.queryAsJson("SELECT * from datatype_test WHERE some_text='Hello'")(_.toList)
      assert(result.head.asInstanceOf[JsObject].value == Map[String, JsValue](
        "id" -> JsNumber(1),
        "key" -> JsNumber(1),
        "some_text" -> JsString("Hello"),
        "some_boolean" -> JsBoolean(true),
        "some_double" -> JsNumber(1.0),
        "some_array" -> JsArray(List(JsString("A"), JsString("B")))))
    }
  }

  describe ("Building the COPY SQL Statement") {

    it ("should work") {
      val result = dataStore.buildCopySql("someRelation", Set("key1", "key2", "id", "anotherKey"))
      assert(result == "COPY someRelation(anotherKey, key1, key2) FROM STDIN CSV")
    }

  }

  describe ("Building the COPY FROM STDIN data") {

    it ("should work") {
      val data = List[JsObject](
       JsObject(Map("key1" -> JsString("hi"), "key2" -> JsString("hello")).toSeq),
       JsObject(Map("key1" -> JsString("hi2"), "key2" -> JsNull).toSeq)
      )
      val strWriter = new StringWriter()
      val resultFile = dataStore.writeCopyData(data.iterator, strWriter)
      val result = strWriter.toString
      assert(result == "\"hi\",\"hello\"\n\"hi2\",\n")
    }
  }

  describe ("Writing to the data store") {

    it("should work") {
      val testRow = JsObject(Map[String, JsValue](
        "key" -> JsNumber(100),
        "some_text" -> JsString("I am sample text."),
        "some_boolean" -> JsBoolean(false),
        "some_double" -> JsNumber(13.37),
        "some_null" -> JsNull,
        "some_array" -> JsArray(List(JsString("13"), JsString("37")))
      ).toSeq)
      dataStore.addBatch(List(testRow).iterator, "datatype_test")
      val result = dataStore.queryAsJson("SELECT * from datatype_test")(_.toList)
      val resultFields = result.head.fields
      val expectedResult = testRow.value.filterKeys(_ != "some_null")
      assert(resultFields.toMap.filterKeys(_ != "id").values.toSet == expectedResult.values.toSet) 
    }
  }

  it("should correctly insert arrays with escape characters") {
    val jsonArr = Json.parse("""["dobj@","@prep_}as","dobj\"@nsubj","dobj@prep_\\","dobj@prep_to"]""")
    val testRow = JsObject(Map[String, JsValue](
        "key" -> JsNull,
        "some_text" -> JsNull,
        "some_boolean" -> JsNull,
        "some_double" -> JsNull,
        "some_null" -> JsNull,
        "some_array" -> jsonArr
      ).toSeq)
    dataStore.addBatch(List(testRow).iterator, "datatype_test")
    val result = dataStore.queryAsJson("SELECT * from datatype_test")(_.toList)
    val resultFields = result.head.fields
    val expectedResult = testRow.value
    assert(resultFields.toMap.filterKeys(_ != "id") == Map("some_array" -> jsonArr)) 
  }
}
  


    
