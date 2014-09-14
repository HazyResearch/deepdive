package org.deepdive.test.unit

import org.deepdive.inference._
import org.deepdive.test._
import org.scalatest._
import org.deepdive.datastore._
import org.deepdive.settings._

class PostgresInferenceDataStoreSpec extends SQLInferenceDataStoreSpec
  with PostgresInferenceDataStoreComponent {

  lazy val inferenceDataStore = new PostgresInferenceDataStore(dbSettings)
  def dataStoreHelper : JdbcDataStore = PostgresDataStore

}
