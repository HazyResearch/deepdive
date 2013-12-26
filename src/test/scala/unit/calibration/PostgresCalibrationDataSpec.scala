package org.deepdive.test.unit

import anorm._
import org.deepdive.test._
import org.deepdive.calibration._
import org.scalatest._

class PostgresCalibrationDataSpec extends FunSpec with BeforeAndAfter 
  with PostgresCalibrationDataComponent {

  implicit lazy val connection = inferenceDataStore.connection

  before {
    PostgresTestDataStore.init()
    SQL("drop schema if exists public cascade; create schema public;").execute()
    inferenceDataStore.init()
    createSampleInferenceRelation()
  }

  private def createSampleInferenceRelation() {
    SQL("""create table t1_c1_inference(id bigserial primary key, c1 boolean, 
      last_sample boolean, probability double precision)""").execute()
    SQL("""insert into t1_c1_inference(c1, last_sample, probability) VALUES
      (null, false, 0.31), (null, true, 0.93), (null, true, 0.97), 
      (false, false, 0.0), (true, true, 0.77), (true, true, 0.81)""").execute()
  }

  describe("Finding all variable relations and attributes") {

    it("should work") {
      SQL("""insert into variables(id, mapping_relation, mapping_column) VALUES
        (0, 'rel1', 'c1'), (1, 'rel2', 'c2')""").execute()
      val result = calibrationData.relationsAndAttributes()
      assert(result == Set(("rel1", "c1"), ("rel2", "c2")))
    }

  }

  describe("Getting prediction counts") {
    it("should work") {
      val buckets = Bucket.ten
      val result = calibrationData.probabilityCounts("t1", "c1", buckets)
      assert(result == buckets.zip(List(1,0,0,1,0,0,0,1,1,2)).toMap)
    }
  }

  describe("Getting precision counts") {
    it("should work") {
      val buckets = Bucket.ten
      val result = calibrationData.predictionCounts("t1", "c1", buckets)
      assert(result == buckets.zip(List(
        (0,1),(0,0),(0,0),(0,0),(0,0),(0,0),(0,0),(1,0),(1,0),(0,0)
      )).toMap)
    }
  }

}

