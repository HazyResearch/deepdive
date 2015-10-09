package org.deepdive.test.unit

import anorm._
import org.deepdive.datastore._
import org.deepdive.extraction._
import org.deepdive.test.helpers._
import org.deepdive.test._
import org.scalatest._
import scala.io.Source
import play.api.libs.json._
import java.io.StringWriter

class PostgresExtractionDataStoreSpec extends FunSpec with BeforeAndAfter
  with PostgresDataStoreComponent {

  lazy implicit val connection = dataStore.borrowConnection()

  val isPostgres = TestHelper.getTestEnv match {
      case TestHelper.Psql | TestHelper.Greenplum => true
      case _ => false
    }
  def cancelUnlessPostgres() = assume(isPostgres)

  before {
    if (isPostgres) {
      JdbcDataStoreObject.init()
      dataStore.init()
      SQL("drop schema if exists public cascade; create schema public;").execute()
      SQL("""create table datatype_test(id bigserial primary key, key integer, some_text text,
        some_boolean boolean, some_double double precision, some_null boolean,
        some_array text[]);""").execute()
    }
  }

  after {
    if (isPostgres) {
      JdbcDataStoreObject.close()
    }
  }

  describe ("Building the COPY SQL Statement") {

    it ("should work") {
      cancelUnlessPostgres()
      val result = dataStore.buildCopySql("someRelation", Set("key1", "key2", "id", "anotherKey"))
      assert(result == "COPY someRelation(anotherKey, key1, key2) FROM STDIN CSV")
    }

  }

  describe ("Building the COPY FROM STDIN data") {

    it ("should work") {
      cancelUnlessPostgres()
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

}




