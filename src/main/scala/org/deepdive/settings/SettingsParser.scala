package org.deepdive.settings

import org.deepdive.Logging
import com.typesafe.config._
import scala.collection.JavaConversions._
import scala.util.Try

object SettingsParser extends Logging {

   def loadFromConfig(rootConfig: Config) : Settings = {
    // Validations makes sure that the supplied config includes all the required settings.
    rootConfig.checkValid(ConfigFactory.defaultReference(), "deepdive")
    val config = rootConfig.getConfig("deepdive")

    // Connection settings
    val schemaSettings = loadSchemaSettings(config)
    val extractors = loadExtractionSettings(config)
    val inferenceSettings = loadInferenceSettings(config)
    val calibrationSettings = loadCalibrationSettings(config)
    val samplerSettings = loadSamplerSettings(config)

    Settings(schemaSettings, extractors, inferenceSettings, 
      calibrationSettings, samplerSettings)
  }

  private def loadSchemaSettings(config: Config) : SchemaSettings = {
    val schemaConfig = Try(config.getConfig("schema")).getOrElse {
      log.warning("No schema defined.")
      return SchemaSettings(Nil.toMap)
    }
    val variableConfig = schemaConfig.getConfig("variables")
    val relations = variableConfig.root.keySet.toList
    val relationsWithConfig = relations.zip(relations.map(variableConfig.getConfig))
    val variableMap = relationsWithConfig.flatMap { case(relation, relationConf) =>
      relationConf.root.keySet.map { attributeName =>
        Tuple2(s"${relation}.${attributeName}", 
          relationConf.getString(attributeName))
      }
    }.toMap
    SchemaSettings(variableMap)
  }

  private def loadExtractionSettings(config: Config) : ExtractionSettings = {
    val extractionConfig = Try(config.getConfig("extraction")).getOrElse {
      return ExtractionSettings(0l, Nil) 
    }
    val initialVariableId = Try(extractionConfig.getLong("initial_vid")).getOrElse(0l)
    val extractors = extractionConfig.getObject("extractors").keySet().map { extractorName =>
      val extractorConfig = extractionConfig.getConfig(s"extractors.$extractorName")
      val outputRelation = extractorConfig.getString("output_relation")
      val inputQuery = InputQueryParser.parse(InputQueryParser.inputQueryExpr, 
        extractorConfig.getString(s"input")).getOrElse {
        throw new RuntimeException(s"parsing ${extractorConfig.getString(s"input")} failed")
      }
      val udf = extractorConfig.getString(s"udf")
      val parallelism = Try(extractorConfig.getInt(s"parallelism")).getOrElse(1)
      val inputBatchSize = Try(extractorConfig.getInt(s"input_batch_size")).getOrElse(10000)
      val outputBatchSize = Try(extractorConfig.getInt(s"output_batch_size")).getOrElse(50000)
      val dependencies = Try(extractorConfig.getStringList("dependencies").toSet).getOrElse(Set())
      Extractor(extractorName, outputRelation, inputQuery, udf, parallelism, 
        inputBatchSize, outputBatchSize, dependencies)
    }.toList
    ExtractionSettings(initialVariableId, extractors)
  }

  private def loadInferenceSettings(config: Config): InferenceSettings = {
    val inferenceConfig = Try(config.getConfig("inference")).getOrElse {
      return InferenceSettings(Nil, None)
    }
    val batchSize = Try(inferenceConfig.getInt("batch_size")).toOption
    val factorConfig = Try(inferenceConfig.getObject("factors")).getOrElse {
      return InferenceSettings(Nil, batchSize)
    }
    val factors = factorConfig.keySet().map { factorName =>
      val factorConfig = inferenceConfig.getConfig(s"factors.$factorName")
      val factorInputQuery = factorConfig.getString("input_query")
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
    InferenceSettings(factors, batchSize)
  }

  private def loadCalibrationSettings(config: Config) : CalibrationSettings = {
    val calibrationConfig = Try(config.getConfig("calibration")).getOrElse { 
      return CalibrationSettings(0.0)
    }
    val holdoutFraction = Try(calibrationConfig.getDouble("holdout_fraction")).getOrElse(0.0)
    CalibrationSettings(holdoutFraction)
  }

  private def loadSamplerSettings(config: Config) : SamplerSettings = {
    val samplingConfig = config.getConfig("sampler")
    val javaArgs = Try(samplingConfig.getString("java_args")).getOrElse("")
    val samplerArgs = samplingConfig.getString("sampler_args")
    SamplerSettings(javaArgs, samplerArgs)
  }

}