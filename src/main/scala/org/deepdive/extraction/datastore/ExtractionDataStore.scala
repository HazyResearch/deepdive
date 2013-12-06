package org.deepdive.extraction.datastore

import spray.json._

/* Stores Extraction Results */
trait ExtractionDataStoreComponent {

  def dataStore : ExtractionDataStore

  trait ExtractionDataStore {
    def writeResult(result: List[JsObject], outputRelation: String) : Unit
  }
  
}