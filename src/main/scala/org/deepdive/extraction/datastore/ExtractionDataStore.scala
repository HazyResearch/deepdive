package org.deepdive.extraction.datastore

import spray.json._

/* Stores Extraction Results */
trait ExtractionDataStoreComponent {

  def dataStore : ExtractionDataStore

  trait ExtractionDataStore {
    def queryAsJson(query: String) : Stream[JsObject]
    def queryAsMap(query: String) : Stream[Map[String, Any]]
    def writeResult(result: List[JsObject], outputRelation: String) : Unit
  }
  
}