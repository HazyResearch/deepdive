package org.deepdive.extraction.datastore

import anorm._
import java.lang.RuntimeException
import org.deepdive.context._
import org.deepdive.settings._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.datastore.Utils._
import org.deepdive.Logging
import spray.json._

trait PostgresExtractionDataStoreComponent extends ExtractionDataStoreComponent  {

  val dataStore = new PostgresExtractionDataStore

  class PostgresExtractionDataStore extends ExtractionDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    /* How many tuples to insert at once */
    val BATCH_SIZE = 1000

    /* 
     * Builds a parameterized INSERT statement for a relation, excluding the id column.
     * Example: "INSERT INTO entities(name, sentence_id) VALUES ({name}, {sentence_id});"
     */
    private def buildInsert(relation: Relation) = {
      val fields = relation.schema.keys.filterNot(_ == "id").toList.sorted
      val relation_fields =  "(" + fields.mkString(", ") + ")"
      val relationPlaceholders =  "(" + fields.map { field =>
        "{" + field + "}"
      }.mkString(", ") + ")"
      s"INSERT INTO ${relation.name} ${relation_fields} VALUES ${relationPlaceholders};"
    }


    def writeResult(result: List[JsObject], outputRelation: String) : Unit = {
      
      val relation = Context.settings.findRelation(outputRelation) match {
        case None => throw new RuntimeException(
          s"relation=${outputRelation} not found in configuration.")
        case Some(relation) => relation
      }

      val insertStatement = buildInsert(relation)
      val sqlStatement = SQL(insertStatement)
      log.debug(s"Writing extraction result to postgres. length=${result.length}, sql=${insertStatement}")
      result.grouped(BATCH_SIZE).zipWithIndex.foreach { case(window, i) =>
        log.debug(s"${BATCH_SIZE * i}/${result.size}")
        // Implicit conversion to Anorm Sequence through Utils
        val batchInsert = new BatchSql(sqlStatement, (relation, window))
        batchInsert.execute()
      }
      log.debug(s"Wrote num=${result.length} records.")
    }
  }

}