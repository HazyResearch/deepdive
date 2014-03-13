// TODO: HSQL currently not supported

// package org.deepdive.test.unit

// import com.typesafe.config._
// import org.deepdive.inference._
// import org.deepdive.test._
// import org.scalatest._
// import org.deepdive.datastore._
// import scalikejdbc._

// class HSQLInferenceDataStoreSpec extends SQLInferenceDataStoreSpec
//   with HSQLInferenceDataStoreComponent {

//   def dataStoreHelper : JdbcDataStore = HSQLDataStore

//   override def init : Unit = {
//     val configurationStr = """
//     deepdive.db.default: {
//       driver: "org.hsqldb.jdbc.JDBCDriver"
//       url: "jdbc:hsqldb:mem:deepdive_test"
//       user: "SA"
//       password: ""
//     }"""
//     val config = ConfigFactory.parseString(configurationStr)
//     JdbcDataStore.init(config)
//     SQL("drop schema if exists public cascade;").execute.apply()
//     inferenceDataStore.init()
//   }

// }
