package org.deepdive.test.integration

import anorm._
import akka.testkit._
import com.typesafe.config._
import org.deepdive.test._
import org.deepdive.Context
import org.deepdive._
import org.deepdive.datastore.{PostgresDataStore, JdbcDataStore}
import org.deepdive.test.helpers.TestHelper
import org.scalatest._
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time.SpanSugar._
import scalikejdbc.ConnectionPool
import scala.sys.process._

// A test for non-termination
class BrokenTest extends FunSpec with TimeLimitedTests {

  val timeLimit = 10000 millis
  val badConfig = ConfigFactory.parseString(getBadConfig).withFallback(ConfigFactory.load)
  
  /** application.conf configuration
   */
  def getBadConfig = TestHelper.getConfig +
    s"""
      deepdive.schema.variables {
        coin.is_correct: BADCONFIG___
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

      deepdive.inference.parallel_grounding: ${System.getenv("PARALLEL_GROUNDING") match {
        case "true" | "1" | "True" | "TRUE" => "true"
        case _ => "false"
      }}
    """
  
  it("should not get stuck for bad configs") {
    
    intercept[RuntimeException] {
      DeepDive.run(badConfig, "out/test_coin")
    }
    // should not timeout (10 seconds)
    Context.system.awaitTermination()
  }

}
