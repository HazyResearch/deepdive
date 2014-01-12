package org.deepdive.extraction.datastore

import org.deepdive.datastore.DataStoreUtils
import play.api.libs.json._

object ExtractionDataStore {
  type JsonExtractionDataStore = ExtractionDataStoreComponent#ExtractionDataStore[_ <: JsValue]
}

/* Stores extraction results and queries the database for extracted data */
trait ExtractionDataStoreComponent {

  def dataStore : ExtractionDataStore[_ <: JsValue]

  /* Stores extraction results and queries the database for extracted data */
  trait ExtractionDataStore[A <: JsValue] {
    
    /* How many extracted tuples to insert at once */
    def BatchSize : Int

    /* Initialize the data store. Must be called before anything else */
    def init() : Unit

    /* 
     * Returns the result of the query as a stream of untyped Maps. 
     * How the query string is interpreted depends on the implementing data store.
     * For example, Postgres interprets the query as a SQL statement
     */
    def queryAsMap[B](query: String)(block: Iterator[Map[String, Any]] => B) : B

    /* Returns the result of the query as a stream of JSON objects */
    def queryAsJson[B](query: String)(block: Iterator[A] => B) : B

    /* 
     * Writes a list of tuples back to the datastore.
     * IMPORTANT: This method must assign a globally unique variable id to each record 
     */
    def addBatch(result: Iterator[JsObject], outputRelation: String) : Unit

  }
  
}