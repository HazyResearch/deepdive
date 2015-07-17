package org.deepdive.test.unit

import com.typesafe.config._
import org.deepdive.settings._
import org.deepdive.Context
import org.scalatest._
import org.deepdive.Logging


class SettingsParserSpec extends FunSpec with PrivateMethodTester with Logging {

  val defaultConfig = ConfigFactory.load().getConfig("deepdive")

  describe("Parsing Schema Settings") {
    it ("should work"){
      val config = ConfigFactory.parseString("""
      schema.variables.relation1.var1 : Boolean
      schema.variables.relation1.var2 : Boolean
      schema.variables.relation2.var3 : Categorical(2)
      """).withFallback(defaultConfig)
      val loadSchemaSettings = PrivateMethod[Settings]('loadSchemaSettings)
      val result = SettingsParser invokePrivate loadSchemaSettings(Settings(config = config))
      assert(result.schemaSettings == SchemaSettings(
        variables = Map("relation1.var1" -> BooleanType,
          "relation1.var2" -> BooleanType,
          "relation2.var3" -> MultinomialType(2)
        )
      ))
    }

    it ("should fail if variable column is missing"){
      val config = ConfigFactory.parseString("""
      schema.variables.relation1 : Boolean
      """).withFallback(defaultConfig)

      val loadSchemaSettings = PrivateMethod[Settings]('loadSchemaSettings)
      var excep=0
      intercept[RuntimeException] {
        val result = SettingsParser invokePrivate loadSchemaSettings(Settings(config = config))
      }
    }

    it ("should fail if variable table is malformed"){
      val config = ConfigFactory.parseString("""
      schema.variables.relation1_4145/24 : Boolean
      """).withFallback(defaultConfig)

      val loadSchemaSettings = PrivateMethod[Settings]('loadSchemaSettings)
      var excep=0
      intercept[RuntimeException] {
        val result = SettingsParser invokePrivate loadSchemaSettings(Settings(config = config))
      }
    }
  }

  describe("Parsing Extractor Settings") {

    it ("should work with json_extractor"){
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
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      assert(result.extractionSettings == ExtractionSettings(
        extractors = List(
          Extractor(
            name = "extractor1",
            style = "json_extractor",
            outputRelation = "entities",
            inputQuery = "SELECT * FROM documents",
            udf = "udf/entities.py",
            parallelism = 4,
            inputBatchSize = 100,
            outputBatchSize = 1000,
            dependencies = Set("extractor2"),
            beforeScript = Option("/bin/cat"),
            afterScript = Option("/bin/dog")
          )
        ),
        parallelism = 5
      ))
    }

    it ("should work with tsv_extractor"){
      val config = ConfigFactory.parseString("""
      extraction.parallelism: 5
      extraction.extractors.extractor2.output_relation: "entities"
      extraction.extractors.extractor2.input: "SELECT * FROM documents"
      extraction.extractors.extractor2.udf: "udf/entities.py"
      extraction.extractors.extractor2.parallelism = 4
      extraction.extractors.extractor2.input_batch_size: 100
      extraction.extractors.extractor2.output_batch_size:1000
      extraction.extractors.extractor2.dependencies:["extractor3"]
      extraction.extractors.extractor2.style: "tsv_extractor"
      extraction.extractors.extractor2.before: "/bin/cat"
      extraction.extractors.extractor2.after: "/bin/dog"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      assert(result.extractionSettings == ExtractionSettings(
        extractors = List(
          Extractor(
            name = "extractor2",
            style = "tsv_extractor",
            outputRelation = "entities",
            inputQuery = "SELECT * FROM documents",
            udf = "udf/entities.py",
            parallelism = 4,
            inputBatchSize = 100,
            outputBatchSize = 1000,
            dependencies = Set("extractor3"),
            beforeScript = Option("/bin/cat"),
            afterScript = Option("/bin/dog")
          )
        ),
        parallelism = 5
      ))
    }

    it ("should work with plpy_extractor"){
      val config = ConfigFactory.parseString("""
      extraction.parallelism: 5
      extraction.extractors.extractor3.output_relation: "entities"
      extraction.extractors.extractor3.input: "SELECT * FROM documents"
      extraction.extractors.extractor3.udf: "udf/entities.py"
      extraction.extractors.extractor3.parallelism = 4
      extraction.extractors.extractor3.input_batch_size: 100
      extraction.extractors.extractor3.output_batch_size:1000
      extraction.extractors.extractor3.dependencies:["extractor4"]
      extraction.extractors.extractor3.style: "plpy_extractor"
      extraction.extractors.extractor3.before: "/bin/cat"
      extraction.extractors.extractor3.after: "/bin/dog"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      assert(result.extractionSettings == ExtractionSettings(
        extractors = List(
          Extractor(
            name = "extractor3",
            style = "plpy_extractor",
            outputRelation = "entities",
            inputQuery = "SELECT * FROM documents",
            udf = "udf/entities.py",
            parallelism = 4,
            inputBatchSize = 100,
            outputBatchSize = 1000,
            dependencies = Set("extractor4"),
            beforeScript = Option("/bin/cat"),
            afterScript = Option("/bin/dog")
          )
        ),
        parallelism = 5
      ))
    }

    it ("should work with cmd_extractor"){
      val config = ConfigFactory.parseString("""
      extraction.extractors.extractor4.cmd: "ls"
      extraction.extractors.extractor4.dependencies:["extractor5"]
      extraction.extractors.extractor4.style: "cmd_extractor"
      extraction.extractors.extractor4.before: "/bin/cat"
      extraction.extractors.extractor4.after: "/bin/dog"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      assert(result.extractionSettings == ExtractionSettings(
        extractors = List(
          Extractor(
            name = "extractor4",
            style = "cmd_extractor",
            dependencies = Set("extractor5"),
            beforeScript = Option("/bin/cat"),
            afterScript = Option("/bin/dog"),
            cmd = Some("ls")
          )
        ),
        parallelism = 1
      ))
    }

    it ("should work with sql_extractor"){
      val config = ConfigFactory.parseString("""
      extraction.extractors.extractor5.sql: "SELECT * FROM documents"
      extraction.extractors.extractor5.dependencies:["extractor6"]
      extraction.extractors.extractor5.style: "sql_extractor"
      extraction.extractors.extractor5.before: "/bin/cat"
      extraction.extractors.extractor5.after: "/bin/dog"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      assert(result.extractionSettings == ExtractionSettings(
        extractors = List(
          Extractor(
            name = "extractor5",
            style = "sql_extractor",
            dependencies = Set("extractor6"),
            beforeScript = Option("/bin/cat"),
            afterScript = Option("/bin/dog"),
            sqlQuery = "SELECT * FROM documents"
          )
        ),
        parallelism = 1
      ))
    }

    it("should fail when the input query is not defined") {
      val config = ConfigFactory.parseString("""
      extraction.extractors.extractor1.output_relation: "entities"
      extraction.extractors.extractor1.udf: "udf/entities.py"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      intercept[Exception] {
        val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      }
    }

    it("should fail when the udf is not defined") {
      val config = ConfigFactory.parseString("""
      extraction.extractors.extractor1.input: "select"
      extraction.extractors.extractor1.output_relation: "entities"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      intercept[Exception] {
        val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      }
    }

    it("should fail when the output_relation is not defined") {
      val config = ConfigFactory.parseString("""
      extraction.extractors.extractor1.input: "select"
      extraction.extractors.extractor1.udf: "udf/entities.py"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      intercept[Exception] {
        val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      }
    }

    it("should fail when the extractor style is not valid") {
      val config = ConfigFactory.parseString("""
      extraction.extractors.extractor1.input: "select"
      extraction.extractors.extractor1.output_relation: "entities"
      extraction.extractors.extractor1.style: "ext"
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      intercept[Exception] {
        val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      }
    }

    it("should work with loader configuration") {
      val config = ConfigFactory.parseString("""
      extraction.parallelism: 5
      extraction.extractors.ext_people {
        parallelism: 6
        input: "SELECT sentence_id, words, ner_tags FROM sentences"
        output_relation: "people_mentions"
        udf: "udf/ext_people.py"
        dependencies: ["ext_create_index_sentences"]
        input_batch_size: 4000
        output_batch_size:1000
        style: "tsv_extractor"
        loader: "ndbloader"
        loader_config: {
          threads: 4
          schema: "udf/people_mentions.loaderschema"
          parallel_transactions: 100
          connection: "127.0.0.1:1186"
        }
      }
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      assert(result.extractionSettings == ExtractionSettings(
        extractors = List(
          Extractor(
            name = "ext_people",
            style = "tsv_extractor",
            outputRelation = "people_mentions",
            inputQuery = "SELECT sentence_id, words, ner_tags FROM sentences",
            udf = "udf/ext_people.py",
            parallelism = 6,
            inputBatchSize = 4000,
            outputBatchSize = 1000,
            dependencies = Set("ext_create_index_sentences"),
            loader = "ndbloader",
            loaderConfig = LoaderConfig(
              connection = "127.0.0.1:1186",
              schemaFile = "udf/people_mentions.loaderschema",
              threads = 4,
              parallelTransactions = 100
            )
          )
        ),
        parallelism = 5
      ))
    }

    it("should work with default loader configuration") {
      val config = ConfigFactory.parseString("""
      extraction.parallelism: 5
      extraction.extractors.ext_people {
        parallelism: 6
        input: "SELECT sentence_id, words, ner_tags FROM sentences"
        output_relation: "people_mentions"
        udf: "udf/ext_people.py"
        dependencies: ["ext_create_index_sentences"]
        input_batch_size: 4000
        output_batch_size:1000
        style: "tsv_extractor"
        loader: "ndbloader"
        loader_config: {
          schema: "udf/people_mentions.loaderschema"
          connection: "127.0.0.1:1186"
        }
      }
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      assert(result.extractionSettings == ExtractionSettings(
        extractors = List(
          Extractor(
            name = "ext_people",
            style = "tsv_extractor",
            outputRelation = "people_mentions",
            inputQuery = "SELECT sentence_id, words, ner_tags FROM sentences",
            udf = "udf/ext_people.py",
            parallelism = 6,
            inputBatchSize = 4000,
            outputBatchSize = 1000,
            dependencies = Set("ext_create_index_sentences"),
            loader = "ndbloader",
            loaderConfig = LoaderConfig(
              connection = "127.0.0.1:1186",
              schemaFile = "udf/people_mentions.loaderschema",
              threads = 6,
              parallelTransactions = 60
            )
          )
        ),
        parallelism = 5
      ))
    }

    it("should fail with wrong loader configuration (no connection)") {
      val config = ConfigFactory.parseString("""
      extraction.parallelism: 5
      extraction.extractors.ext_people {
        parallelism: 6
        input: "SELECT sentence_id, words, ner_tags FROM sentences"
        output_relation: "people_mentions"
        udf: "udf/ext_people.py"
        dependencies: ["ext_create_index_sentences"]
        input_batch_size: 4000
        output_batch_size:1000
        style: "tsv_extractor"
        loader: "ndbloader"
        loader_config: {
          schema: "udf/people_mentions.loaderschema"
        }
      }
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      intercept[Exception] {
        val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
      }
    }

    it("should fail with wrong loader configuration (no schema)") {
      val config = ConfigFactory.parseString("""
      extraction.parallelism: 5
      extraction.extractors.ext_people {
        parallelism: 6
        input: "SELECT sentence_id, words, ner_tags FROM sentences"
        output_relation: "people_mentions"
        udf: "udf/ext_people.py"
        dependencies: ["ext_create_index_sentences"]
        input_batch_size: 4000
        output_batch_size:1000
        style: "tsv_extractor"
        loader: "ndbloader"
        loader_config: {
          connection: "127.0.0.1:1186"
        }
      }
      """).withFallback(defaultConfig)
      val loadExtractionSettings = PrivateMethod[Settings]('loadExtractionSettings)
      intercept[Exception] {
        val result = SettingsParser invokePrivate loadExtractionSettings(Settings(config = config))
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
      val loadInferenceSettings = PrivateMethod[Settings]('loadInferenceSettings)
      val result = SettingsParser invokePrivate loadInferenceSettings(Settings(config = config))
      assert(result.inferenceSettings == InferenceSettings(
        factors = List(
          FactorDesc(
            name = "factor1",
            inputQuery = "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id",
            func = ImplyFactorFunction(
              variables = Seq(
                FactorFunctionVariable(
                  relation = "a", field = "is_present", isArray = false
                )
              )
            ),
            weight = UnknownFactorWeight(Nil),
            weightPrefix = "factor1"
          )
        ),
        insertBatchSize = Some(100000),
        skipLearning = false,
        weightTable = ""
      ))
    }

    it("should throw an exception when there's a syntax error") {
      val config = ConfigFactory.parseString("""
      inference.factors.factor1.input_query = "SELECT a.*, b.* FROM a INNER JOIN b ON a.document_id = b.id"
      inference.factors.factor1.function: ":)))"
      inference.factors.factor1.weight: "?"
      """).withFallback(defaultConfig)
      val loadInferenceSettings = PrivateMethod[Settings]('loadInferenceSettings)
      intercept[RuntimeException] {
        val result = SettingsParser invokePrivate loadInferenceSettings(Settings(config = config))
      }
    }

  }

  describe("Parsing Calibration Settings") {
    it ("should work when specified") {
      val config = ConfigFactory.parseString("""
        calibration.holdout_fraction: 0.25
        calibration.holdout_query: "SELECT 0;"
        calibration.observation_query: "SELECT 1;"
      """)
      val loadCalibrationSettings = PrivateMethod[Settings]('loadCalibrationSettings)
      val result = SettingsParser invokePrivate loadCalibrationSettings(Settings(config = config))
      assert(result.calibrationSettings == CalibrationSettings(
        holdoutFraction = 0.25,
        holdoutQuery = Option("SELECT 0;"),
        observationQuery = Option("SELECT 1;")
      ))
    }

    it ("should work when not specified") {
      val loadCalibrationSettings = PrivateMethod[Settings]('loadCalibrationSettings)
      val result = SettingsParser invokePrivate loadCalibrationSettings(Settings(config = ConfigFactory.empty()))
      assert(result.calibrationSettings == CalibrationSettings())
    }
  }

  describe("Parsing Sampler Settings") {
    it ("should work when specified") {
      val config = ConfigFactory.parseString("""
        sampler.sampler_cmd = "java -jar util/sampler-assembly-0.1.jar"
        sampler.sampler_args = "-i 1000"
      """).withFallback(defaultConfig)
      val loadSamplerSettings = PrivateMethod[Settings]('loadSamplerSettings)
      val result = SettingsParser invokePrivate loadSamplerSettings(Settings(config = config))
      assert(result.samplerSettings == SamplerSettings(
        samplerCmd = "java -jar util/sampler-assembly-0.1.jar",
        samplerArgs = "-i 1000"
      ))
    }

    it ("should work when not specified") {
      val config = ConfigFactory.parseString("").withFallback(defaultConfig)
      val loadSamplerSettings = PrivateMethod[Settings]('loadSamplerSettings)
      val result = SettingsParser invokePrivate loadSamplerSettings(Settings(config = config))
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
      val loadPipelineSettings = PrivateMethod[Settings]('loadPipelineSettings)
      val result = SettingsParser invokePrivate loadPipelineSettings(Settings(config = config))
      assert(result.pipelineSettings == PipelineSettings(
        activePipelineName = Some("p1"),
        pipelines = List(
          Pipeline(id = "p1", tasks = Set("f1", "f2")),
          Pipeline("p2", Set("f2", "f3"))
        )
      ))
      assert(result.pipelineSettings.activePipeline.get == Pipeline("p1", Set("f1", "f2")))
    }

    it ("should work when not specified") {
      val loadPipelineSettings = PrivateMethod[Settings]('loadPipelineSettings)
      val result = SettingsParser invokePrivate loadPipelineSettings(Settings(config = ConfigFactory.empty()))
      assert(result.pipelineSettings == PipelineSettings(
        activePipelineName = None,
        pipelines = Nil
      ))
    }

    it ("should work when relearn_from") {
      val config = ConfigFactory.parseString("""
        pipeline.relearn_from: "/PATH_TO_DEEPDIVE_HOME/out/2014-05-02T131658/"
      """)
      val loadPipelineSettings = PrivateMethod[Settings]('loadPipelineSettings)
      val result = SettingsParser invokePrivate loadPipelineSettings(Settings(config = config))
      assert(result.pipelineSettings == PipelineSettings(
        activePipelineName = None,
        pipelines = Nil,
        relearnFrom = "/PATH_TO_DEEPDIVE_HOME/out/2014-05-02T131658/"
      ))
    }
  }


}
