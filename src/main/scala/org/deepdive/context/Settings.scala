package org.deepdive.context

import com.typesafe.config._
import org.deepdive.context.parsing._
import scala.collection.JavaConversions._
import scala.util.Try

case class Connection(host: String, port: Int, db: String, user: String, password: String)
case class ForeignKey(childRelation: String, childAttribute: String, parentRelation: String, 
  parentAttribute: String)
case class Relation(name: String, schema: Map[String, String], foreignKeys: List[ForeignKey], evidenceField: Option[String])
case class EtlTask(relation: String, source: String)
case class Extractor(name:String, outputRelation: String, inputQuery: String, udf: String, 
  factor: Option[Factor])
case class Factor(name: String, func: FactorFunction, weight: FactorWeight)
case class Settings(connection: Connection, relations: List[Relation], etlTasks: List[EtlTask], 
  extractors: List[Extractor])

sealed trait FactorWeight {
  def variables : List[String]
}
case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
}
case class UnknownFactorWeight(variables: List[String]) extends FactorWeight

sealed trait FactorFunction {
  def variables : Seq[String]
}
case class ImplyFactorFunction(head: String, body: Seq[String]) extends FactorFunction {
  def variables = Seq(head) ++ body 
}

object Settings {
 
  private var _settings : Settings = null;

  def getRelation(name: String) : Option[Relation] = _settings.relations.find(_.name == name)
  def getExtractor(name: String) : Option[Extractor] = _settings.extractors.find(_.name == name)
  def extractorForRelation(name: String) : Option[Extractor] = 
    _settings.extractors.find(_.outputRelation == name)

  def getRelationParents(name: String) : List[String] = {
    getRelation(name).map(_.foreignKeys.map(_.parentRelation)).getOrElse(Nil).filter(_ != name)
      .flatMap(Settings.getRelationParents)
  }
  
  // TODO: Generate database URL
  def databaseUrl : String = {
    val c = _settings.connection
    s"jdbc:postgresql://${c.host}:${c.port}/${c.db}"
  }

  def get() : Settings = _settings

  def loadDefault() = loadFromConfig(ConfigFactory.load)

  def loadFromConfig(rootConfig: Config) {
    // Validations makes sure that the supplied config includes all the required settings.
    rootConfig.checkValid(ConfigFactory.defaultReference(), "deepdive")
    val config = rootConfig.getConfig("deepdive")

    // Connection settings
    val connection = Connection(
      config.getString("global.connection.host"),
      config.getInt("global.connection.port"),
      config.getString("global.connection.db"),
      config.getString("global.connection.user"),
      config.getString("global.connection.password")
    )

    // Relation Settings
    // ==================================================

    val relations = config.getObject("relations").keySet().map { relationName =>
      val relationConfig = config.getConfig(s"relations.$relationName")
      
      // Schema
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


    // ETL Settings
    // ==================================================

    val etlTasks = config.getObject("ingest").keySet().map { relationName =>
      val source = config.getString(s"ingest.$relationName.source")
      EtlTask(relationName, source)
    }.toList


    // Extractor Settings
    // ==================================================
    val extractors = config.getObject("extractions").keySet().map { extractorName =>
      val extractorConfig = config.getConfig(s"extractions.$extractorName")
      val outputRelation = extractorConfig.getString("output_relation")
      val inputQuery = extractorConfig.getString(s"input")
      val udf = extractorConfig.getString(s"udf")
      val factor = Try(Factor(
        config.getString(s"extractions.$extractorName.factor.name"),
        FactorFunctionParser.parse(FactorFunctionParser.factorFunc, 
          config.getString(s"extractions.$extractorName.factor.function")).get,
        FactorWeightParser.parse(FactorWeightParser.factorWeight,
           config.getString(s"extractions.$extractorName.factor.weight")).get 
      )).toOption
      Extractor(extractorName, outputRelation, inputQuery, udf, factor)
    }.toList


    _settings = Settings(connection, relations, etlTasks, extractors)
  }

}