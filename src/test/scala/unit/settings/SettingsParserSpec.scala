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

  describe("Parsing Schema Settings") {
    it ("should work"){
      val config = ConfigFactory.parseString("""
      schema.variables.relation1.var1 : Boolean
      schema.variables.relation1.var2 : Boolean
      schema.variables.relation2.var3 : Boolean
      """)
      val loadSchemaSettings = PrivateMethod[SchemaSettings]('loadSchemaSettings)
      val result = SettingsParser invokePrivate loadSchemaSettings(config)
      assert(result == SchemaSettings(
        Map("relation1.var1" -> "Boolean",
          "relation1.var2" -> "Boolean",
          "relation2.var3" -> "Boolean")))
    }
  }

  describe("Parsing Extractor Settings") {
    it ("should work"){
      val config = ConfigFactory.parseString("""
      extraction.initial_vid: 100
      extraction.extractors.extractor1.output_relation: "entities"
      extraction.extractors.extractor1.input: "SELECT * FROM documents"
      extraction.extractors.extractor1.udf: "udf/entities.py"
      extraction.extractors.extractor1.parallelism = 4
      extraction.extractors.extractor1.input_batch_size = 100
      extraction.extractors.extractor1.output_batch_size = 1000
      extraction.extractors.extractor1.dependencies = ["extractor2"]
      """)
      val loadExtractionSettings = PrivateMethod[ExtractionSettings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(config)
      assert(result == ExtractionSettings(100, List(
        Extractor("extractor1", "entities", "SELECT * FROM documents", "udf/entities.py", 
          4, 100, 1000, Set("extractor2")))))
    }
  }

  describe("Parsing Inference Settings") {
    it ("should work"){
      val config = ConfigFactory.parseString("""
      inference.batch_size: 100000
      inference.factors.factor1.input_query = "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id"
      inference.factors.factor1.function: "a.is_present = Imply()"
      inference.factors.factor1.weight: "?"
      """)
      val loadInferenceSettings = PrivateMethod[InferenceSettings]('loadInferenceSettings)
      val result = SettingsParser invokePrivate loadInferenceSettings(config)
      assert(result == InferenceSettings(List(FactorDesc("factor1", 
        "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id",
        ImplyFactorFunction(FactorFunctionVariable("a", "is_present", false), Nil), 
        UnknownFactorWeight(Nil), "factor1")), Option(100000)))
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