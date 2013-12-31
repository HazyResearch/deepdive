package org.deepdive.test

import com.typesafe.config.ConfigFactory
import org.deepdive.datastore.PostgresDataStore

  object PostgresTestDataStore {
    
    val config = ConfigFactory.load()
    lazy val databaseUrl = config.getString("deepdive.test.database_url")
    lazy val databaseUser = config.getString("deepdive.test.database_user")
    lazy val databasePassword = config.getString("deepdive.test.database_password") 

    def init() {
      PostgresDataStore.init(databaseUrl, databaseUser, databasePassword)
    }
}