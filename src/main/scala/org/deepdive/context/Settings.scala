package org.deepdive.context

import com.typesafe.config._
import org.deepdive._
import org.deepdive.context.parsing._
import scala.collection.JavaConversions._

case class Connection(host: String, port: Int, db: String, user: String, password: String)
case class Relation(name: String, schema: Map[String, String])
case class EtlTask(relation: String, source: String)
case class Extractor(name:String, outputRelation: String, inputQuery: String, udf: String, factor: Factor)
case class Factor(name: String, func: FactorFunction, weight: String)

class Settings(config: Config) {

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
    Relation(relationName,schema.toMap.mapValues(_.toString))
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
      config.getString(s"deepdive.extractions.$extractorName.factor.weight")
    )
    Extractor(extractorName, outputRelation, inputQuery, udf, factor)
  }


}