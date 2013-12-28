package org.deepdive.settings

import com.typesafe.config._
import scala.collection.JavaConversions._
import scala.util.Try

object SettingsParser {

   def loadFromConfig(rootConfig: Config) : Settings = {
    // Validations makes sure that the supplied config includes all the required settings.
    rootConfig.checkValid(ConfigFactory.defaultReference(), "deepdive")
    val config = rootConfig.getConfig("deepdive")

    // Connection settings
    val connection = loadConnection(config)
    val schemaSettings = loadSchemaSettings(config)
    val extractors = loadExtractionSettings(config)
    val factors = loadFactors(config)
    val calibrationSettings = loadCalibrationSettings(config)
    val samplerSettings = loadSamplerSettings(config)

    Settings(connection, schemaSettings, extractors, factors, 
      calibrationSettings, samplerSettings)
  }

  private def loadConnection(config: Config) : Connection = {
    Connection(
      config.getString("connection.url"),
      config.getString("connection.user"),
      config.getString("connection.password")
    )
  }

  private def loadSchemaSettings(config: Config) : SchemaSettings = {
    Try(config.getConfig("schema")).map { schemaConfig =>
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
    }.getOrElse(SchemaSettings(Map()))
  }

  private def loadExtractionSettings(config: Config) : ExtractionSettings = {
    val extractionConfig = config.getConfig("extraction")
    val initialVariableId = Try(extractionConfig.getLong("initial_vid")).getOrElse(0l)
    val extractors = extractionConfig.getObject("extractors").keySet().map { extractorName =>
      val extractorConfig = extractionConfig.getConfig(s"extractors.$extractorName")
      val outputRelation = extractorConfig.getString("output_relation")
      val inputQuery = extractorConfig.getString(s"input")
      val udf = extractorConfig.getString(s"udf")
      val parallelism = Try(extractorConfig.getInt(s"parallelism")).getOrElse(1)
      val batchSize = Try(extractorConfig.getInt(s"batch_size")).getOrElse(1000)
      val dependencies = Try(extractorConfig.getStringList("dependencies").toSet).getOrElse(Set())
      Extractor(extractorName, outputRelation, inputQuery, udf, parallelism, batchSize, dependencies)
    }.toList
    ExtractionSettings(initialVariableId, extractors)
  }

  private def loadFactors(config: Config): List[FactorDesc] = {
    Try(config.getObject("factors").keySet().map { factorName =>
      val factorConfig = config.getConfig(s"factors.$factorName")
      val factorInputQuery = factorConfig.getString("input_query")
      val factorFunction = FactorFunctionParser.parse(
        FactorFunctionParser.factorFunc, factorConfig.getString("function"))
      val factorWeight = FactorWeightParser.parse(
        FactorWeightParser.factorWeight, factorConfig.getString("weight"))
      val factorWeightPrefix = Try(factorConfig.getString("weightPrefix")).getOrElse(factorName)
      FactorDesc(factorName, factorInputQuery, factorFunction.get, 
        factorWeight.get, factorWeightPrefix)
    }.toList).getOrElse(Nil)
  }

  private def loadCalibrationSettings(config: Config) : CalibrationSettings = {
    Try(config.getConfig("calibration")).map { calibrationConfig =>
      val holdoutFraction = Try(calibrationConfig.getDouble("holdout_fraction")).getOrElse(0.0)
      CalibrationSettings(holdoutFraction)
    }.getOrElse(CalibrationSettings(0.0))
  }

  private def loadSamplerSettings(config: Config) : SamplerSettings = {
    val defaultSamplerSettings = SamplerSettings("-Xmx4g", "-l 1000 -s 10 -i 1000 -t 4")
    Try(config.getConfig("sampler")).map { samplingConfig =>
      val javaArgs = Try(samplingConfig.getString("java_args")).getOrElse(defaultSamplerSettings.javaArgs)
      val samplerArgs = Try(samplingConfig.getString("sampler_args")).getOrElse(defaultSamplerSettings.samplerArgs)
      SamplerSettings(javaArgs, samplerArgs)
    }.getOrElse(defaultSamplerSettings)
  }

}