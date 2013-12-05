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

    Settings(connection, relations, etlTasks, extractors)
  }

  private def loadConnection(config: Config) : Connection = {
    Connection(
      config.getString("global.connection.host"),
      config.getInt("global.connection.port"),
      config.getString("global.connection.db"),
      config.getString("global.connection.user"),
      config.getString("global.connection.password")
    )
  }

  private def loadRelations(config: Config) : List[Relation] = {
    config.getObject("relations").keySet().map { relationName =>
      val relationConfig = config.getConfig(s"relations.$relationName")
      val schema = relationConfig.getObject("schema").unwrapped
      val foreignKeys = Try(relationConfig.getObject("fkeys").keySet().map { childAttr =>
        val Array(parentRelation, parentAttribute) = relationConfig.getString(s"fkeys.${childAttr}").split('.')
        ForeignKey(relationName, childAttr, parentRelation, parentAttribute)
      }).getOrElse(Nil).toList
      // We add "id" as a key so that it can be used as a variable. 
      // TODO: Rename foreign keys to something more appropriate
      val allKeys = foreignKeys :+ ForeignKey(relationName, "id", relationName, "id")
      // Evidence
      val evidence = Try(relationConfig.getString("evidence_field")).toOption
      Relation(relationName,schema.toMap.mapValues(_.toString), allKeys, evidence)
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
      val factor = Try(FactorDesc(
        config.getString(s"extractions.$extractorName.factor.name"),
        FactorFunctionParser.parse(FactorFunctionParser.factorFunc, 
          config.getString(s"extractions.$extractorName.factor.function")).get,
        FactorWeightParser.parse(FactorWeightParser.factorWeight,
           config.getString(s"extractions.$extractorName.factor.weight")).get 
      )).toOption
      Extractor(extractorName, outputRelation, inputQuery, udf, factor)
    }.toList
  }

}