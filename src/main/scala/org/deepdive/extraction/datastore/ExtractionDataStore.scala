package org.deepdive.extraction.datastore

import org.deepdive.datastore.DataStoreUtils
import spray.json._

/* Stores extraction results and queries the database for extracted data */
trait ExtractionDataStoreComponent {

  def dataStore : ExtractionDataStore

  /* Stores extraction results and queries the database for extracted data */
  trait ExtractionDataStore {

    /* How many extracted tuples to insert at once */
    def BatchSize : Int

    /* 
     * Returns the result of the query as a stream of untyped Maps. 
     * How the query string is interpreted depends on the implementing data store.
     * For example, Postgres interprets the query as a SQL statement
     */
    def queryAsMap(query: String) : Stream[Map[String, Any]]

    /* Returns the result of the query as a stream of JSON objects */
    def queryAsJson(query: String) : Stream[JsObject]

    /* 
     * Writes a list of tuples back to the datastore.
     * IMPORTANT: This method must assign a globally unique variable id to each record 
     */
    def write(result: Seq[JsObject], outputRelation: String) : Unit

  }
  
}