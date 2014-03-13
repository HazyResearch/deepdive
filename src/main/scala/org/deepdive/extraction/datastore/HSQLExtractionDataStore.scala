package org.deepdive.extraction.datastore

import scalikejdbc._
import org.deepdive.datastore.HSQLDataStore
import org.deepdive.Logging
import play.api.libs.json._


trait HSQLExtractionDataStoreComponent extends ExtractionDataStoreComponent {
  val dataStore = new HSQLExtractionDataStore
}


class HSQLExtractionDataStore extends JdbcExtractionDataStore with Logging {

  def ds = HSQLDataStore

  def init() = {
    log.debug("Initializing HSQL data store...")
  }

  def addBatch(result: Iterator[JsObject], outputRelation: String) : Unit = {
    ds.DB.localTx { implicit session =>
      HSQLDataStore.bulkInsertJSON(outputRelation, result)
    }
  }

}