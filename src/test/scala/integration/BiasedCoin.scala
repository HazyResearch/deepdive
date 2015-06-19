package org.deepdive.test.integration

import anorm._
import com.typesafe.config._
import org.deepdive.test._
import org.deepdive.Context
import org.deepdive._
import org.deepdive.datastore._
import org.deepdive.test.helpers.TestHelper
import org.scalatest._
import scalikejdbc.ConnectionPool

class BiasedCoin extends FunSpec {

  val config = ConfigFactory.parseString(getConfig).withFallback(ConfigFactory.load)

  /** insert data into db
   */
  def prepareData() {
    JdbcDataStoreObject.init(config)
    JdbcDataStoreObject.withConnection { implicit conn =>
      TestHelper.getTestEnv() match {
        case TestHelper.Psql =>
          SQL("drop schema if exists public cascade; create schema public;").execute()
        case _ =>
      }

      SQL("""create table coin(is_correct boolean, id bigint);""").execute()
      SQL("""insert into coin(is_correct) values
        (true), (true), (true), (true),
        (true), (true), (true), (true),
        (false),
        (NULL), (NULL), (NULL), (NULL),
        (NULL), (NULL), (NULL), (NULL),
        (NULL);""").execute()
    }
    JdbcDataStoreObject.close()
  }

  /** application.conf configuration
   */
  def getConfig = TestHelper.getConfig +
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

      deepdive.sampler.sampler_args: "-l 2000 -i 500 -s 1 --alpha 0.1 --diminish 0.99 --quiet"
    """


  it("should work") {

    prepareData()

    DeepDive.run(config, "out/test_coin")
    JdbcDataStoreObject.init(config)
    JdbcDataStoreObject.withConnection { implicit conn =>

      // get learned weight
      val weight = SQL("select weight from dd_inference_result_weights;")().head[Double]("weight")

      // weight = log(#positive) / log(#negative) ~= 2.1
      assert(weight > 1.9 && weight < 2.3)

      // get inference results, probability should be around 8/9
      val inference = SQL("""select count(*) as c from (select expectation from dd_inference_result_variables
        where expectation > 0.94 or expectation < 0.84) tmp;""")().head[Long]("c")

      assert(inference === 0)

    }
    JdbcDataStoreObject.close()
  }


}
