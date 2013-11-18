package org.deepdive.context

import com.typesafe.config._
import org.deepdive._
import org.deepdive.context.parsing._
import scala.collection.JavaConversions._

case class Connection(host: String, port: Int, db: String, user: String, password: String)
case class ForeignKey(childRelation: String, childAttribute: String, parentRelation: String, parentAttribute: String)
case class Relation(name: String, schema: Map[String, String], foreignKeys: List[ForeignKey])
case class EtlTask(relation: String, source: String)
case class Extractor(name:String, outputRelation: String, inputQuery: String, udf: String, factor: Factor)
case class Factor(name: String, func: FactorFunction, weight: FactorWeight)
case class Settings(connection: Connection, relations: List[Relation], etlTasks: List[EtlTask], extractors: List[Extractor])

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
case class ImplyFactorFunction(variables: Seq[String]) extends FactorFunction

object Settings {
 
  private var _settings : Settings = null;

  def getRelation(name: String) : Option[Relation] = _settings.relations.find(_.name == name)
  def getExtractor(name: String) : Option[Extractor] = _settings.extractors.find(_.name == name)
  // TODO: Generate database URL
  def databaseUrl = "jdbc:postgresql://localhost/deepdive_paleo"


  def get() : Settings = _settings

  def loadDefault() = loadFromConfig(ConfigFactory.load)

  def loadFromConfig(config: Config) {
    // Validations makes sure that the supplied config includes all the required settings.
    config.checkValid(ConfigFactory.defaultReference(), "deepdive")

    // Connection settings
    val connection = Connection(
      config.getString("deepdive.global.connection.host"),
      config.getInt("deepdive.global.connection.port"),
      config.getString("deepdive.global.connection.db"),
      config.getString("deepdive.global.connection.user"),
      config.getString("deepdive.global.connection.password")
    )

    // Schema Settings
    val relations = config.getObject("deepdive.relations").keySet().map { relationName =>
      val schema =  config.getObject(s"deepdive.relations.$relationName.schema").unwrapped
      val foreignKeys = config.getObject(s"deepdive.relations.$relationName.fkeys").keySet().map { childAttr =>
        val Array(parentRelation, parentAttribute) = 
          config.getString(s"deepdive.relations.$relationName.fkeys.$childAttr").split(".")
        ForeignKey(relationName, childAttr, parentRelation, parentAttribute)
      }.toList
      Relation(relationName,schema.toMap.mapValues(_.toString), foreignKeys)
    }.toList

    val etlTasks = config.getObject("deepdive.ingest").keySet().map { relationName =>
      val source = config.getString(s"deepdive.ingest.$relationName.source")
      EtlTask(relationName, source)
    }.toList

    val extractors = config.getObject("deepdive.extractions").keySet().map { extractorName =>
      val outputRelation = config.getString(s"deepdive.extractions.$extractorName.output_relation")
      val inputQuery = config.getString(s"deepdive.extractions.$extractorName.input")
      val udf = config.getString(s"deepdive.extractions.$extractorName.udf")
      val factor = Factor(
        config.getString(s"deepdive.extractions.$extractorName.factor.name"),
        FactorFunctionParser.parse(FactorFunctionParser.factorFunc, 
          config.getString(s"deepdive.extractions.$extractorName.factor.function")).get,
        FactorWeightParser.parse(FactorWeightParser.factorWeight,
           config.getString(s"deepdive.extractions.$extractorName.factor.weight")).get 
      )
      Extractor(extractorName, outputRelation, inputQuery, udf, factor)
    }.toList
    _settings = Settings(connection, relations, etlTasks, extractors)
  }

}