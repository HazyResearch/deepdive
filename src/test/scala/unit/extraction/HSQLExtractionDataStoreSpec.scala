package org.deepdive.test.unit

import anorm._
import com.typesafe.config._
import java.io.StringWriter
import org.deepdive.datastore._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.test._
import org.scalatest._
import play.api.libs.json._
import scala.io.Source

class HSQLExtractionDataStoreSpec extends FunSpec with BeforeAndAfter
  with HSQLExtractionDataStoreComponent {

  lazy implicit val connection = PostgresDataStore.borrowConnection()

  val configurationStr = """
    deepdive.db.default: {
      driver: "org.hsqldb.jdbc.JDBCDriver"
      url: "jdbc:hsqldb:mem:deepdive_test"
      user: "SA"
      password: ""
    }"""

  before {
    val config = ConfigFactory.parseString(configurationStr)
    JdbcDataStore.init(config)
    dataStore.init()
    //SQL("drop schema if exists public cascade; create schema public;").execute()
    SQL("""drop table if exists datatype_test;""").execute()
    SQL("""create table datatype_test(id bigint identity primary key, key integer, some_text longvarchar, 
      some_boolean boolean, some_double double precision, some_null boolean, 
      some_array longvarchar array, some_longtext clob);""").execute()
  }

  after {
    JdbcDataStore.close()
  }

  describe("Querying as a Map") {

    def insertSampleData() = {
      SQL("""insert into datatype_test(key) VALUES (1), (2), (3), (4)""").execute()
    }

    it("should work for simple attributes") {
      insertSampleData()
      val result = dataStore.queryAsMap("SELECT key from datatype_test;")(_.toList)
      assert(result.head === Map("KEY" -> 1))
    }

    it("should work for alaised attributes") {
      insertSampleData()
      val result = dataStore.queryAsMap("SELECT key AS \"d1.key2\" from datatype_test;")(_.toList)
      assert(result.head === Map("d1.key2" -> 1))
    }

    it("should work for aggregated attributes") {
      insertSampleData()
      val result = dataStore.queryAsMap("""SELECT COUNT(*) AS num 
        from DATATYPE_TEST GROUP BY key""")(_.toList)
      assert(result.head === Map("NUM" -> 1))
    }

    it("should work with empty tables") {
      val result = dataStore.queryAsMap("SELECT * from datatype_test;")(_.toList)
      assert(result == Nil)
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
      SQL("""insert into datatype_test(key, some_text, some_boolean, some_double, some_array, some_longtext) 
        VALUES 
          (1, 'Hello', true, 1.0, ARRAY['A', 'B'], '東京'), 
          (1, 'Ce', false, 2.3, ARRAY['C', 'D'], '東京')""").execute()
    }

    it("should work with aggregate data types") {
      insertSampleRow()
      val result = dataStore.queryAsJson(
        """SELECT key, array_agg(some_text) AS "datatype_test.texts"
        FROM datatype_test GROUP BY key"""
      )(_.toList)
      assert(result.head.asInstanceOf[JsObject].value == Map[String, JsValue](
        "KEY" -> JsNumber(1),
        "datatype_test.texts" -> JsArray(Seq(JsString("Hello"), JsString("Ce")))
      ))
    }

    it("should work with simple data types") {
      insertSampleRow()
      val result = dataStore.queryAsJson("SELECT * from datatype_test")(_.toList)
      assert(result.head.asInstanceOf[JsObject].value == Map[String, JsValue](
        "ID" -> JsNumber(0),
        "KEY" -> JsNumber(1),
        "SOME_TEXT" -> JsString("Hello"),
        "SOME_BOOLEAN" -> JsBoolean(true),
        "SOME_DOUBLE" -> JsNumber(1.0),
        "SOME_ARRAY" -> JsArray(List(JsString("A"), JsString("B"))),
        "SOME_LONGTEXT" -> JsString("東京")
      ))
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
        "some_array" -> JsArray(List(JsString("13"), JsString("37"))),
        "some_longtext" -> JsString("東京\na\nb\\c")
      ).toSeq)
      dataStore.addBatch(List(testRow).iterator, "datatype_test")
      val result = dataStore.queryAsMap("SELECT * from datatype_test")(_.toList)
      val resultFields = result.head
      val expectedResult = testRow.value
      assert(resultFields.filterKeys(_ != "ID") === Map[String, Any](
        "KEY" -> 100,
        "SOME_TEXT" -> "I am sample text.",
        "SOME_BOOLEAN" -> false,
        "SOME_DOUBLE" -> 13.37,
        "SOME_ARRAY" -> List("13", "37"),
        "SOME_LONGTEXT" -> "東京\na\nb\\c"
      )) 
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
    val result = dataStore.queryAsMap("SELECT * from datatype_test")(_.toList)
    val resultFields = result.head
    val expectedResult = testRow.value
    assert(resultFields.filterKeys(_ != "ID") === Map[String, Any](
        "SOME_ARRAY" -> List("dobj@","@prep_}as","dobj\"@nsubj","dobj@prep_\\","dobj@prep_to")
      )) 
  }
}
  


    