package org.deepdive.settings

import org.deepdive.Logging
import org.deepdive.Context
import org.deepdive.helpers.Helpers
import com.typesafe.config._
import scala.collection.JavaConversions._
import scala.util.Try

object SettingsParser extends Logging {

   def loadFromConfig(rootConfig: Config) : Settings = {
    val config = rootConfig.getConfig("deepdive")

    val dbSettings = loadDbSettings(config)
    val schemaSettings = loadSchemaSettings(config)
    val extractors = loadExtractionSettings(config)
    val inferenceSettings = loadInferenceSettings(config)
    val calibrationSettings = loadCalibrationSettings(config)
    val samplerSettings = loadSamplerSettings(config)
    val pipelineSettings = loadPipelineSettings(config)

    // Make sure that the variables related to the Greenplum distributed
    // filesystem are set if the user wants to use parallel grounding
    if (inferenceSettings.parallelGrounding) {
      if (dbSettings.gphost == "" || dbSettings.gpport == "" || dbSettings.gppath == "") {
        throw new RuntimeException(s"inference.parallelGrounding is set to true, but one of db.default.gphost, db.default.gpport, or db.default.gppath is not specified")
      }
    }

    Settings(schemaSettings, extractors, inferenceSettings, 
      calibrationSettings, samplerSettings, pipelineSettings, dbSettings)
  }

  // Returns: case class DbSetting(driver: String, url: String, user: String, password: String, dbname: String, host: String, port: String)
  private def loadDbSettings(config: Config) : DbSettings = {
    val dbConfig = Try(config.getConfig("db.default")).getOrElse {
      log.warning("No schema defined.")
      return DbSettings(Helpers.PsqlDriver, null, null, null, null, null, null, null, null, null)
    }
    val driver = Try(dbConfig.getString("driver")).getOrElse(null)
    val url = Try(dbConfig.getString("url")).getOrElse(null)
    val user = Try(dbConfig.getString("user")).getOrElse(null)
    val password = Try(dbConfig.getString("password")).getOrElse(null)
    val dbname = Try(dbConfig.getString("dbname")).getOrElse(null)
    val host = Try(dbConfig.getString("host")).getOrElse(null)
    val port = Try(dbConfig.getString("port")).getOrElse(null)
    val gphost = Try(dbConfig.getString("gphost")).getOrElse("")
    val gpport = Try(dbConfig.getString("gpport")).getOrElse("")
    var gppath = Try(dbConfig.getString("gppath")).getOrElse("")
    if (gppath.takeRight(1) == "/") gppath = gppath.take(gppath.length -1)
    log.info(s"Database settings: user ${user}, dbname ${dbname}, host ${host}, port ${port}.")
    if (gphost != "") {
      log.info(s"GPFDIST settings: host ${gphost} port ${gpport} path ${gppath}")
    }
    return DbSettings(driver, url, user, password, dbname, host, port, gphost, gppath, gpport)
  }


  private def loadSchemaSettings(config: Config) : SchemaSettings = {
    val schemaConfig = Try(config.getConfig("schema")).getOrElse {
      log.warning("No schema defined.")
      return SchemaSettings(Nil.toMap, None)
    }
    val variableConfig = schemaConfig.getConfig("variables")
    val relations = variableConfig.root.keySet.toList
    val relationsWithConfig = relations.zip(relations.map(variableConfig.getConfig))
    val variableMap = relationsWithConfig.flatMap { case(relation, relationConf) =>
      relationConf.root.keySet.map { attributeName =>
        val dataTypeStr = relationConf.getString(attributeName)
        val dataType = DataTypeParser.parse(DataTypeParser.dataType, dataTypeStr).getOrElse {
          throw new RuntimeException(s"Unknown data type: ${dataTypeStr}")
        }
        Tuple2(s"${relation}.${attributeName}", dataType)
      }
    }.toMap
    val setupFile = Try(schemaConfig.getString("setup"))
    SchemaSettings(variableMap, setupFile.toOption)
  }

  private def loadExtractionSettings(config: Config) : ExtractionSettings = {
    val extractionConfig = Try(config.getConfig("extraction")).getOrElse {
      return ExtractionSettings(Nil, 1)
    }
    val extractorParallelism = Try(extractionConfig.getInt("parallelism")).getOrElse(1)
    val extractors = extractionConfig.getObject("extractors").keySet().map { extractorName =>
      val extractorConfig = extractionConfig.getConfig(s"extractors.$extractorName")
      val style = Try(extractorConfig.getString(s"style")).getOrElse("json_extractor")
      style match {
        case "json_extractor" | "plpy_extractor" | "tsv_extractor" =>
          val outputRelation = extractorConfig.getString ("output_relation")
          val inputQuery = InputQueryParser.parse (InputQueryParser.inputQueryExpr,
            extractorConfig.getString (s"input") ).getOrElse {
            throw new RuntimeException (s"parsing ${
              extractorConfig.getString (s"input")
            } failed")
          }
          val udf = extractorConfig.getString (s"udf")
          val sqlQuery = Try (extractorConfig.getString (s"sql") ).getOrElse ("")
          val cmd = Try (extractorConfig.getString ("cmd") ).toOption
          val parallelism = Try (extractorConfig.getInt (s"parallelism") ).getOrElse (1)
          val inputBatchSize = Try (extractorConfig.getInt (s"input_batch_size") ).getOrElse (10000)
          val outputBatchSize = Try (extractorConfig.getInt (s"output_batch_size") ).getOrElse (50000)
          val dependencies = Try (extractorConfig.getStringList ("dependencies").toSet).getOrElse (Set () )
          val beforeScript = Try (extractorConfig.getString ("before") ).toOption
          val afterScript = Try (extractorConfig.getString ("after") ).toOption
          val loader = Try (extractorConfig.getString ("loader") ).getOrElse("")
          val loaderConfigObj = Try (extractorConfig.getConfig ("loader_config") ).getOrElse(null)
          val loaderConfig = loaderConfigObj match {
            case null => null
            case _ => LoaderConfig (
                loaderConfigObj.getString("connection"),
                loaderConfigObj.getString("schema"),
                Try(loaderConfigObj.getInt("threads")).getOrElse(parallelism),
                Try(loaderConfigObj.getInt("parallel_transactions")).getOrElse(60)
            )
          }
          Extractor(extractorName, style, outputRelation, inputQuery, udf, parallelism,
            inputBatchSize, outputBatchSize, dependencies, beforeScript, afterScript, sqlQuery, cmd,
            loader, loaderConfig)

        case "sql_extractor" | "cmd_extractor" =>
          val sqlQuery = Try (extractorConfig.getString (s"sql") ).getOrElse ("")
          val cmd = Try (extractorConfig.getString ("cmd") ).toOption
          val dependencies = Try (extractorConfig.getStringList ("dependencies").toSet).getOrElse (Set () )
          val beforeScript = Try (extractorConfig.getString ("before") ).toOption
          val afterScript = Try (extractorConfig.getString ("after") ).toOption
          Extractor(extractorName, style, "", null, "", 1,
            10000, 50000, dependencies, beforeScript, afterScript, sqlQuery, cmd)
      }
    }.toList
    ExtractionSettings(extractors, extractorParallelism)
  }

  private def loadInferenceSettings(config: Config): InferenceSettings = {
    val inferenceConfig = Try(config.getConfig("inference")).getOrElse {
      return InferenceSettings(Nil, None, false, "", false)
    }
    // These configs are currently disabled in grounding
    // // Zifei's changes: Add capability to skip learning
    val skipLearning = Try(inferenceConfig.getBoolean("skip_learning")).getOrElse(false)
    val weightTable = Try(inferenceConfig.getString("weight_table")).getOrElse("")
    val parallelGrounding = Try(inferenceConfig.getBoolean("parallel_grounding")).getOrElse(false)
    // val skipLearning = false
    // val weightTable = ""

    // if (!weightTable.isEmpty() && skipLearning == false) {
    //   log.error("inference.skip_learning must be true when inference.weight_table is assigned!")
    //   throw new RuntimeException(s"skip_learning assertion failed")
    // }

    val batchSize = Try(inferenceConfig.getInt("batch_size")).toOption
    val factorConfig = Try(inferenceConfig.getObject("factors")).getOrElse {
      return InferenceSettings(Nil, batchSize, skipLearning, weightTable, parallelGrounding)
    }
    val factors = factorConfig.keySet().map { factorName =>
      val factorConfig = inferenceConfig.getConfig(s"factors.$factorName")
      val factorInputQuery = InputQueryParser.parse(InputQueryParser.DatastoreInputQueryExpr, 
        factorConfig.getString("input_query")).getOrElse {
        throw new RuntimeException(s"parsing ${factorConfig.getString("input_query")} failed")
      }.query
      val factorFunction = FactorFunctionParser.parse(
        FactorFunctionParser.factorFunc, factorConfig.getString("function")).getOrElse {
        throw new RuntimeException(s"parsing ${factorConfig.getString("function")} failed")
      }
      val factorWeight = FactorWeightParser.parse(
        FactorWeightParser.factorWeight, factorConfig.getString("weight")).getOrElse {
        throw new RuntimeException(s"parsing ${factorConfig.getString("weight")} failed")
      }
      val factorWeightPrefix = Try(factorConfig.getString("weightPrefix")).getOrElse(factorName)
      FactorDesc(factorName, factorInputQuery, factorFunction, 
          factorWeight, factorWeightPrefix)
    }.toList
    InferenceSettings(factors, batchSize, skipLearning, weightTable, parallelGrounding)
  }

  private def loadCalibrationSettings(config: Config) : CalibrationSettings = {
    val calibrationConfig = Try(config.getConfig("calibration")).getOrElse { 
      return CalibrationSettings(0.0, None, None)
    }
    val holdoutFraction = Try(calibrationConfig.getDouble("holdout_fraction")).getOrElse(0.0)
    val holdoutQuery = Try(calibrationConfig.getString("holdout_query")).toOption
    val observationQuery = Try(calibrationConfig.getString("observation_query")).toOption

    CalibrationSettings(holdoutFraction, holdoutQuery, observationQuery)
  }

  private def loadSamplerSettings(config: Config) : SamplerSettings = {
    val samplingConfig = config.getConfig("sampler")

    // Parse Default sampler command based on mac / linux
    val samplerCmd = samplingConfig.getString("sampler_cmd") match {
      case "__DEFAULT__" =>
        val osname = System.getProperty("os.name")
        log.info(s"Detected OS: ${osname}")
        if (osname.startsWith("Linux")) {
          s"${Context.deepdiveHome}/util/sampler-dw-linux gibbs"
        }
        else {
          s"${Context.deepdiveHome}/util/sampler-dw-mac gibbs"
        }
      case _ => samplingConfig.getString("sampler_cmd")
    }

    
    // Zifei's changes: Add capability to skip learning
    // If skip learning, set "-l 0" in sampler
    val skipLearning = Try(config.getConfig("inference").getBoolean("skip_learning")).getOrElse(false)
    val samplerArgs = skipLearning match {
      case false => 
        samplingConfig.getString("sampler_args")
      case true =>
        samplingConfig.getString("sampler_args").replaceAll("""-l +[0-9]+ *""", """-l 0 """)
    }
    log.debug(s"samplerArgs: ${samplerArgs}")
    
    SamplerSettings(samplerCmd, samplerArgs)
    
  }

  private def loadPipelineSettings(config: Config) : PipelineSettings = {
    val pipelineConfig = Try(config.getConfig("pipeline")).getOrElse {
      return PipelineSettings(None, Nil, null)
    }
    val relearnFrom = Try(pipelineConfig.getString("relearn_from")).getOrElse(null)
    val activePipeline = Try(pipelineConfig.getString("run")).toOption
    if (relearnFrom == null) {
      val pipelinesObj = Try(pipelineConfig.getObject("pipelines")).getOrElse {
        return PipelineSettings(None, Nil, null)
      }
      val pipelines = pipelinesObj.keySet().map { pipelineName =>
        val tasks = pipelineConfig.getStringList(s"pipelines.$pipelineName").toSet
        Pipeline(pipelineName, tasks)
      }.toList
      return PipelineSettings(activePipeline, pipelines, relearnFrom)
    }
    PipelineSettings(activePipeline, Nil, relearnFrom)
  }
}
