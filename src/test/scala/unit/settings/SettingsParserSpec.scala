package org.deepdive.test.unit

import com.typesafe.config._
import org.deepdive.settings._
import org.deepdive.Context
import org.scalatest._

class SettingsParserSpec extends FunSpec with PrivateMethodTester {
  
  describe("Parsing Connection Settings") {
    it ("should work") {
      val config = ConfigFactory.parseString("""
      connection.url: "jdbc:postgresql://localhost/deepdive_test"
      connection.user: "deepdive"
      connection.password: "password"
      """)
      val loadConnection = PrivateMethod[Connection]('loadConnection)
      val result = SettingsParser invokePrivate loadConnection(config)
      assert(result === Connection("jdbc:postgresql://localhost/deepdive_test", "deepdive", "password"))
    }
  }

  describe("Parsing Extractor Settings") {
    it ("should work"){
      val config = ConfigFactory.parseString("""
      extractions.extractor1.output_relation: "entities"
      extractions.extractor1.input: "SELECT * FROM documents"
      extractions.extractor1.udf: "udf/entities.py"
      extractions.extractor1.parallelism = 4
      extractions.extractor1.batch_size = 100
      extractions.extractor1.dependencies = ["extractor2"]
      """)
      val loadExtractors = PrivateMethod[List[Extractor]]('loadExtractors)
      val result = SettingsParser invokePrivate loadExtractors(config)
      assert(result == List(
        Extractor("extractor1", "entities", "SELECT * FROM documents", "udf/entities.py", 
          4, 100, Set("extractor2"))))
    }
  }

  describe("Parsing Factor Settings") {
    it ("should work"){
      val config = ConfigFactory.parseString("""
      factors.factor1.input_query = "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id"
      factors.factor1.function: "a.is_present = Imply()"
      factors.factor1.weight: "?"
      """)
      val loadFactors = PrivateMethod[List[FactorDesc]]('loadFactors)
      val result = SettingsParser invokePrivate loadFactors(config)
      assert(result == List(FactorDesc("factor1", 
        "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id",
        ImplyFactorFunction(FactorFunctionVariable("a", "is_present"), Nil), 
        UnknownFactorWeight(Nil))))
    }
  }

  describe("Parsing Calibration Settings") {
    it ("should work when specified") {
      val config = ConfigFactory.parseString("""calibration.holdout_fraction: 0.25""")
      val loadCalibrationSettings = PrivateMethod[CalibrationSettings]('loadCalibrationSettings)
      val result = SettingsParser invokePrivate loadCalibrationSettings(config)
      assert(result == CalibrationSettings(0.25))
    }

    it ("should work when not specified") {
      val loadCalibrationSettings = PrivateMethod[CalibrationSettings]('loadCalibrationSettings)
      val result = SettingsParser invokePrivate loadCalibrationSettings(ConfigFactory.parseString(""))
      assert(result == CalibrationSettings(0))
    }
  }

  describe("Parsing Sampler Settings") {
    it ("should work when specified") {
      val config = ConfigFactory.parseString("""
        sampler.java_args = "-Xmx8g"
        sampler.sampler_args = "-i 1000"
      """)
      val loadSamplerSettings = PrivateMethod[SamplerSettings]('loadSamplerSettings)
      val result = SettingsParser invokePrivate loadSamplerSettings(config)
      assert(result == SamplerSettings("-Xmx8g", "-i 1000"))
    }
    
    it ("should work when not specified") {
      val config = ConfigFactory.parseString("")
      val loadSamplerSettings = PrivateMethod[SamplerSettings]('loadSamplerSettings)
      val result = SettingsParser invokePrivate loadSamplerSettings(config)
      assert(result == SamplerSettings("-Xmx4g", "-l 1000 -s 10 -i 1000 -t 4"))
    }
  }


}