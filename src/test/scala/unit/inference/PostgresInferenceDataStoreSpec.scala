package org.deepdive.test.unit

import org.deepdive.inference._
import org.deepdive.test._
import org.scalatest._
import org.deepdive.datastore._
import org.deepdive.settings._

import java.io._
import org.deepdive.Logging
import akka.actor._
import akka.testkit._
import org.deepdive.datastore._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.extraction.ProcessExecutor._
import org.deepdive.settings._
import org.deepdive.helpers.Helpers
import org.scalatest._
import scala.concurrent.duration._
import play.api.libs.json._
import scalikejdbc._


class PostgresInferenceDataStoreSpec extends SQLInferenceDataStoreSpec
  with PostgresInferenceDataStoreComponent with Logging {

  // Override with a postgres-specific dbSettings
  override val dbSettings = DbSettings(Helpers.PsqlDriver, null, System.getenv("PGUSER"), 
      null, System.getenv("DBNAME"), System.getenv("PGHOST"), 
      System.getenv("PGPORT"), null, null, null)

  lazy val inferenceDataStore = new PostgresInferenceDataStore(dbSettings)
  def dataStoreHelper: JdbcDataStore = PostgresDataStore

}
