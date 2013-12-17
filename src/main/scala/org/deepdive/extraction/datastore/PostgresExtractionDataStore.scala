package org.deepdive.extraction.datastore

import anorm._
import java.lang.RuntimeException
import org.deepdive.context._
import org.deepdive.settings._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.datastore.Utils._
import org.deepdive.Logging
import spray.json._
import spray.json.DefaultJsonProtocol._

trait PostgresExtractionDataStoreComponent extends ExtractionDataStoreComponent {

  val dataStore = new PostgresExtractionDataStore

  class PostgresExtractionDataStore extends ExtractionDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    /* How many tuples to insert at once */
    val BATCH_SIZE = 5000

    /* 
     * Builds a parameterized INSERT statement for a relation, excluding the id column.
     * Example: "INSERT INTO entities(name, sentence_id) VALUES ({name}, {sentence_id});"
     */
    private def buildInsert(relationName: String, keys: Set[String]) = {
      val fields =keys.filterNot(_ == "id").toList.sorted
      val relation_fields =  "(" + fields.mkString(", ") + ")"
      val relationPlaceholders =  "(" + fields.map { field =>
        "{" + field + "}"
      }.mkString(", ") + ")"
      s"INSERT INTO ${relationName} ${relation_fields} VALUES ${relationPlaceholders};"
    }


    def writeResult(result: List[JsObject], outputRelation: String) : Unit = {
      // We sample the keys to figure out which fields to insert into
      // This is a ugly, but using this we don't need an explicit schema definition.
      // Is there a better way?
      val sampledKeys = result.take(BATCH_SIZE).flatMap(_.fields.keySet).toSet

      val insertStatement = buildInsert(outputRelation, sampledKeys)
      val sqlStatement = SQL(insertStatement)
      log.info(s"Writing extraction result to postgres. length=${result.length}, sql=${insertStatement}")
      result.grouped(BATCH_SIZE).zipWithIndex.foreach { case(window, i) =>
        log.debug(s"${BATCH_SIZE * i}/${result.size}")
        // Implicit conversion to Anorm Sequence through Utils
        val batchInsert = new BatchSql(sqlStatement, window)
        batchInsert.execute()
      }
      log.info(s"Wrote num=${result.length} records.")
    }

    def queryAsMap(query: String) : Stream[Map[String, Any]] = {
      SQL(query)().map { row =>
        row.asMap.toMap
      }
    }

    def queryAsJson(query: String) : Stream[JsObject] = {
      queryAsMap(query).map { row =>
        JsObject(row.mapValues(valToJson))
      }
    }

    private def valToJson(x: Any) : JsValue = x match {
      case Some(x) => valToJson(x)
      case None | null => JsNull
      case x : String => x.toJson
      case x : Boolean => x.toJson
      case x : Int => x.toJson
      case x : Long => x.toJson
      case x : Double => x.toJson
      case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray().asInstanceOf[Array[_]].map(valToJson).toJson
      case x =>
        log.error("Could not convert ${x.toString} of type=${x.getClass.name} to JSON")
        JsNull
    }

  }

}