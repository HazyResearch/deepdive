package org.deepdive.test.unit

import org.deepdive.inference._
import org.deepdive.test._
import org.scalatest._
import org.deepdive.datastore._

class PostgresInferenceDataStoreSpec extends SQLInferenceDataStoreSpec
  with PostgresInferenceDataStoreComponent {

  def dataStoreHelper : JdbcDataStore = PostgresDataStore

}
