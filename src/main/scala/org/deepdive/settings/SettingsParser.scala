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
    val relations = loadRelations(config)
    val etlTasks = loadEtlTasks(config)
    val extractors = loadExtractors(config)
    val factors = loadFactors(config)
    val calibrationSettings = loadCalibrationSettings(config)

    Settings(connection, relations, etlTasks, extractors, factors, calibrationSettings)
  }

  private def loadConnection(config: Config) : Connection = {
    Connection(
      config.getString("global.connection.url"),
      config.getString("global.connection.user"),
      config.getString("global.connection.password")
    )
  }

  private def loadRelations(config: Config) : List[Relation] = {
    config.getObject("relations").keySet().map { relationName =>
      val relationConfig = config.getConfig(s"relations.$relationName")
      val schema = relationConfig.getObject("schema").unwrapped
      Relation(relationName,schema.toMap.mapValues(_.toString))
    }.toList
  }

  private def loadEtlTasks(config: Config) : List[EtlTask] = {
    Try(config.getObject("ingest").keySet().map { relationName =>
      val source = config.getString(s"ingest.$relationName.source")
      EtlTask(relationName, source)
    }.toList).getOrElse(Nil)
  }

  private def loadExtractors(config: Config) : List[Extractor] = {
    config.getObject("extractions").keySet().map { extractorName =>
      val extractorConfig = config.getConfig(s"extractions.$extractorName")
      val outputRelation = extractorConfig.getString("output_relation")
      val inputQuery = extractorConfig.getString(s"input")
      val udf = extractorConfig.getString(s"udf")
      val parallelism = Try(extractorConfig.getInt(s"parallelism")).getOrElse(1)
      val batchSize = Try(extractorConfig.getInt(s"batch_size")).getOrElse(1000)
      val dependencies = Try(extractorConfig.getStringList("dependencies").toSet).getOrElse(Set())
      Extractor(extractorName, outputRelation, inputQuery, udf, parallelism, batchSize, dependencies)
    }.toList
  }

  private def loadFactors(config: Config): List[FactorDesc] = {
    Try(config.getObject("factors").keySet().map { factorName =>
      val factorConfig = config.getConfig(s"factors.$factorName")
      val factorInputQuery = factorConfig.getString("input_query")
      val factorFunction = FactorFunctionParser.parse(
        FactorFunctionParser.factorFunc, factorConfig.getString("function"))
      val factorWeight = FactorWeightParser.parse(
        FactorWeightParser.factorWeight, factorConfig.getString("weight"))
      FactorDesc(factorName, factorInputQuery, factorFunction.get, factorWeight.get)
    }.toList).getOrElse(Nil)
  }

  private def loadCalibrationSettings(config: Config) : CalibrationSettings = {
    Try(config.getConfig("calibration")).map { calibrationConfig =>
      val holdoutFraction = Try(calibrationConfig.getDouble("holdout_fraction")).getOrElse(0.0)
      CalibrationSettings(holdoutFraction)
    }.getOrElse(CalibrationSettings(0.0))
  }

}