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
      schema.variables.relation2.var3 : Categorical(2)
      """).withFallback(defaultConfig)
      val loadSchemaSettings = PrivateMethod[SchemaSettings]('loadSchemaSettings)
      val result = SettingsParser invokePrivate loadSchemaSettings(config)
      assert(result == SchemaSettings(
        Map("relation1.var1" -> BooleanType,
          "relation1.var2" -> BooleanType,
          "relation2.var3" -> MultinomialType(2)), None))
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
        Extractor("extractor1", "json_extractor", "entities", "SELECT * FROM documents", "udf/entities.py", 
          4, 100, 1000, Set("extractor2"), Option("/bin/cat"), Option("/bin/dog"), "", None)), 5))
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
      inference.factors.factor1.function: "Imply(a.is_present)"
      inference.factors.factor1.weight: "?"
      """).withFallback(defaultConfig)
      val loadInferenceSettings = PrivateMethod[InferenceSettings]('loadInferenceSettings)
      val result = SettingsParser invokePrivate loadInferenceSettings(config)
      assert(result == InferenceSettings(List(FactorDesc("factor1", 
        "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id",
        ImplyFactorFunction(Seq(FactorFunctionVariable("a", "is_present", false))), 
        UnknownFactorWeight(Nil), "factor1")), Option(100000), false, ""))
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
      val config = ConfigFactory.parseString("""
        calibration.holdout_fraction: 0.25
        calibration.holdout_query: "SELECT 0;"
      """)
      val loadCalibrationSettings = PrivateMethod[CalibrationSettings]('loadCalibrationSettings)
      val result = SettingsParser invokePrivate loadCalibrationSettings(config)
      assert(result == CalibrationSettings(0.25, Option("SELECT 0;")))
    }

    it ("should work when not specified") {
      val loadCalibrationSettings = PrivateMethod[CalibrationSettings]('loadCalibrationSettings)
      val result = SettingsParser invokePrivate loadCalibrationSettings(ConfigFactory.parseString(""))
      assert(result == CalibrationSettings(0, None))
    }
  }

  describe("Parsing Sampler Settings") {
    it ("should work when specified") {
      val config = ConfigFactory.parseString("""
        sampler.sampler_cmd = "java -jar util/sampler-assembly-0.1.jar"
        sampler.sampler_args = "-i 1000"
      """).withFallback(defaultConfig)
      val loadSamplerSettings = PrivateMethod[SamplerSettings]('loadSamplerSettings)
      val result = SettingsParser invokePrivate loadSamplerSettings(config)
      assert(result == SamplerSettings("java -jar util/sampler-assembly-0.1.jar", "-i 1000"))
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

    it ("should work when relearn_from") {
      val config = ConfigFactory.parseString("""
        pipeline.relearn_from: "/PATH_TO_DEEPDIVE_HOME/out/2014-05-02T131658/"
      """)
      val loadPipelineSettings = PrivateMethod[PipelineSettings]('loadPipelineSettings)
      val result = SettingsParser invokePrivate loadPipelineSettings(config)
      assert(result == PipelineSettings(None, List(), "/PATH_TO_DEEPDIVE_HOME/out/2014-05-02T131658/"
      ))
    }
  }


}