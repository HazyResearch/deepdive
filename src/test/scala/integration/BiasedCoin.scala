package org.deepdive.test.integration

import anorm._ 
import com.typesafe.config._
import org.deepdive.test._
import org.deepdive.Context
import org.deepdive._
import org.deepdive.datastore.{PostgresDataStore, JdbcDataStore}
import org.scalatest._
import scalikejdbc.ConnectionPool

class BiasedCoin extends FunSpec {

  def prepareData() {
    JdbcDataStore.init(ConfigFactory.load)
    PostgresDataStore.withConnection { implicit conn =>
      SQL("drop schema if exists public cascade; create schema public;").execute()
      SQL("""create table coin(is_correct boolean, id bigint);""").execute()
      SQL("""insert into coin(is_correct) values 
        (true), (true), (true), (true),
        (true), (true), (true), (true),
        (false),  
        (NULL), (NULL), (NULL), (NULL),
        (NULL), (NULL), (NULL), (NULL),
        (NULL);""").execute()
    }
    JdbcDataStore.close()
  }

  def getConfig = {
    s"""
      deepdive.schema.variables {
        coin.is_correct: Boolean
      }

      deepdive.extraction.extractors: {
      }

      deepdive.inference.factors {
        test {
          input_query: ${"\"\"\""}select id as "coin.id", is_correct as "coin.is_correct" from coin${"\"\"\""}
          function: "IsTrue(coin.is_correct)"
          weight: "?"
        }
      }

    """
  }

  it("should work") {
    prepareData()
    val config = ConfigFactory.parseString(getConfig).withFallback(ConfigFactory.load)
    DeepDive.run(config, "out/test_coin")
  //   // Make sure the data is in the database
    JdbcDataStore.init(ConfigFactory.load)
    PostgresDataStore.withConnection { implicit conn =>
      
    val weight = SQL("select weight from dd_inference_result_weights;")().head[Double]("weight")

    // weight = log(#positive) / log(#negative)
    assert(weight > 1.9 && weight < 2.3)

    val inference = SQL("""select count(*) as c from (select expectation from dd_inference_result_variables 
      where expectation > 0.94 or expectation < 0.84) tmp;""")().head[Long]("c")

    assert(inference === 0)

    }
    JdbcDataStore.close()
  }


}