package org.deepdive.test.unit

import com.typesafe.config._
import org.deepdive.settings._
import org.deepdive.Context
import org.scalatest._

class SettingsParserSpec extends FunSpec with PrivateMethodTester {
  
  val defaultConfig = ConfigFactory.load().getConfig("deepdive")
  
  describe("Parsing Schema Settings") {
    it ("should work"){
      val config = ConfigFactory.parseString("""
      schema.variables.relation1.var1 : Boolean
      schema.variables.relation1.var2 : Boolean
      schema.variables.relation2.var3 : Boolean
      """).withFallback(defaultConfig)
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
      extraction.parallelism: 5
      extraction.extractors.extractor1.output_relation: "entities"
      extraction.extractors.extractor1.input: "SELECT * FROM documents"
      extraction.extractors.extractor1.udf: "udf/entities.py"
      extraction.extractors.extractor1.parallelism = 4
      extraction.extractors.extractor1.input_batch_size: 100
      extraction.extractors.extractor1.output_batch_size:1000
      extraction.extractors.extractor1.dependencies:["extractor2"]
      extraction.extractors.extractor1.before: "/bin/cat"
      extraction.extractors.extractor1.after: "/bin/dog"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[ExtractionSettings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(config)
      assert(result == ExtractionSettings(List(
        Extractor("extractor1", "entities", "SELECT * FROM documents", "udf/entities.py", 
          4, 100, 1000, Set("extractor2"), Option("/bin/cat"), Option("/bin/dog"))), 5))
    }

    it("should fail when the input query is not defined") {
      val config = ConfigFactory.parseString("""
      extraction.extractors.extractor1.output_relation: "entities"
      extraction.extractors.extractor1.udf: "udf/entities.py"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[ExtractionSettings]('loadExtractionSettings)
      intercept[Exception] {
        val result = SettingsParser invokePrivate loadExtractionSettings(config)
      }
    }
  }

  describe("Parsing Inference Settings") {
    
    it ("should work"){
      val config = ConfigFactory.parseString("""
      inference.batch_size: 100000
      inference.factors.factor1.input_query = "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id"
      inference.factors.factor1.function: "a.is_present = Imply()"
      inference.factors.factor1.weight: "?"
      """).withFallback(defaultConfig)
      val loadInferenceSettings = PrivateMethod[InferenceSettings]('loadInferenceSettings)
      val result = SettingsParser invokePrivate loadInferenceSettings(config)
      assert(result == InferenceSettings(List(FactorDesc("factor1", 
        "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id",
        ImplyFactorFunction(FactorFunctionVariable("a", "is_present", false), Nil), 
        UnknownFactorWeight(Nil), "factor1")), Option(100000)))
    }

    it("should throw an exception when there's a syntax error") {
      val config = ConfigFactory.parseString("""
      inference.factors.factor1.input_query = "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id"
      inference.factors.factor1.function: ":)))"
      inference.factors.factor1.weight: "?"
      """).withFallback(defaultConfig)
      val loadInferenceSettings = PrivateMethod[InferenceSettings]('loadInferenceSettings)
      intercept[RuntimeException] {
        val result = SettingsParser invokePrivate loadInferenceSettings(config)
      }
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
      """).withFallback(defaultConfig)
      val loadSamplerSettings = PrivateMethod[SamplerSettings]('loadSamplerSettings)
      val result = SettingsParser invokePrivate loadSamplerSettings(config)
      assert(result == SamplerSettings("-Xmx8g", "-i 1000"))
    }
    
    it ("should work when not specified") {
      val config = ConfigFactory.parseString("").withFallback(defaultConfig)
      val loadSamplerSettings = PrivateMethod[SamplerSettings]('loadSamplerSettings)
      val result = SettingsParser invokePrivate loadSamplerSettings(config)
      assert(result != null)
    }
  }

  describe("Parsing Pipelines Settings") {
    it ("should work when specified") {
      val config = ConfigFactory.parseString("""
        pipeline.run: p1
        pipeline.pipelines {
          p1 : ["f1", "f2"]
          p2 : ["f2", "f3"]
        }
      """)
      val loadPipelineSettings = PrivateMethod[PipelineSettings]('loadPipelineSettings)
      val result = SettingsParser invokePrivate loadPipelineSettings(config)
      assert(result == PipelineSettings(Some("p1"), 
        List(Pipeline("p1", Set("f1", "f2")), Pipeline("p2", Set("f2", "f3")))
      ))
      assert(result.activePipeline.get == Pipeline("p1", Set("f1", "f2")))
    }

    it ("should work when not specified") {
      val loadPipelineSettings = PrivateMethod[PipelineSettings]('loadPipelineSettings)
      val result = SettingsParser invokePrivate loadPipelineSettings(ConfigFactory.parseString(""))
      assert(result == PipelineSettings(None, Nil))
    }
  }


}