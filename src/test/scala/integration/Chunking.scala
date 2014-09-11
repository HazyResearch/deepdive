package org.deepdive.test.integration

import anorm._ 
import com.typesafe.config._
import org.deepdive.test._
import org.deepdive.Context
import org.deepdive._
import org.deepdive.datastore.{PostgresDataStore, JdbcDataStore}
import org.scalatest._
import scalikejdbc.ConnectionPool

class ChunkingApp extends FunSpec {

  def prepareData() {
    JdbcDataStore.init(ConfigFactory.load)
    PostgresDataStore.withConnection { implicit conn =>
    }
    JdbcDataStore.close()
  }

  it("should work") {
    prepareData()
    // val config = ConfigFactory.parseFile("${getClass.getResource("/chunking/application.conf").getFile}").withFallback(ConfigFactory.load)
    // DeepDive.run(config, "out/test_crf")
    // Make sure the data is in the database
    JdbcDataStore.init(ConfigFactory.load)
    PostgresDataStore.withConnection { implicit conn =>
    }
    JdbcDataStore.close()
  }


}