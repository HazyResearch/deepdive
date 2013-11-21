package org.deepdive.context

import com.typesafe.config._
import org.deepdive.context.parsing._
import scala.collection.JavaConversions._

case class Connection(host: String, port: Int, db: String, user: String, password: String)
case class ForeignKey(childRelation: String, childAttribute: String, parentRelation: String, 
  parentAttribute: String)
case class Relation(name: String, schema: Map[String, String], foreignKeys: List[ForeignKey])
case class EtlTask(relation: String, source: String)
case class Extractor(name:String, outputRelation: String, inputQuery: String, udf: String, 
  factor: Factor)
case class Factor(name: String, func: FactorFunction, weight: FactorWeight)
case class Evidence(relation: String, factor: String)
case class Settings(connection: Connection, relations: List[Relation], etlTasks: List[EtlTask], 
  extractors: List[Extractor], evidence: List[Evidence])

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
  def variables = body :+ head
}

object Settings {
 
  private var _settings : Settings = null;

  def getRelation(name: String) : Option[Relation] = _settings.relations.find(_.name == name)
  def getExtractor(name: String) : Option[Extractor] = _settings.extractors.find(_.name == name)
  // TODO: Generate database URL
  def databaseUrl = "jdbc:postgresql://localhost/deepdive_paleo"


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

    // Schema Settings
    val relations = config.getObject("relations").keySet().map { relationName =>
      val schema =  config.getObject(s"relations.$relationName.schema").unwrapped

      val foreignKeys = Option(config.hasPath(s"relations.$relationName.fkeys")).filter(_ == true).map { x =>
        config.getObject(s"relations.$relationName.fkeys").keySet().map { childAttr =>
          val Array(parentRelation, parentAttribute) = 
            config.getString(s"relations.$relationName.fkeys.$childAttr").split(".")
          ForeignKey(relationName, childAttr, parentRelation, parentAttribute)
        }.toList
      }.getOrElse(Nil)
      Relation(relationName,schema.toMap.mapValues(_.toString), foreignKeys)
    }.toList

    val etlTasks = config.getObject("ingest").keySet().map { relationName =>
      val source = config.getString(s"ingest.$relationName.source")
      EtlTask(relationName, source)
    }.toList

    val extractors = config.getObject("extractions").keySet().map { extractorName =>
      val outputRelation = config.getString(s"extractions.$extractorName.output_relation")
      val inputQuery = config.getString(s"extractions.$extractorName.input")
      val udf = config.getString(s"extractions.$extractorName.udf")
      val factor = Factor(
        config.getString(s"extractions.$extractorName.factor.name"),
        FactorFunctionParser.parse(FactorFunctionParser.factorFunc, 
          config.getString(s"extractions.$extractorName.factor.function")).get,
        FactorWeightParser.parse(FactorWeightParser.factorWeight,
           config.getString(s"extractions.$extractorName.factor.weight")).get 
      )
      Extractor(extractorName, outputRelation, inputQuery, udf, factor)
    }.toList

    val evidence = config.getObject("evidence").keySet().map { evidenceName =>
      val relation = config.getString(s"evidence.$evidenceName.relation")
      val factorName = config.getString(s"evidence.$evidenceName.factor")
      Evidence(relation, factorName)
    }.toList

    _settings = Settings(connection, relations, etlTasks, extractors, evidence)
  }

}