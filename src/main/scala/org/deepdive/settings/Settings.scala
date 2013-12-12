package org.deepdive.settings

import com.typesafe.config._
import scala.collection.JavaConversions._
import scala.util.Try


object Settings {
  def loadFromConfig(config: Config) = SettingsParser.loadFromConfig(config)
  def loadDefault() = loadFromConfig(ConfigFactory.load)
}

trait SettingsImpl {

  def connection : Connection
  def relations : List[Relation]
  def etlTasks : List[EtlTask]
  def extractors : List[Extractor]
  def factors : List[FactorDesc]

  def findRelation(name: String) : Option[Relation] = relations.find(_.name == name)
  
  def findExtractor(name: String) : Option[Extractor] = extractors.find(_.name == name)
  
  def findExtractorForRelation(name: String) : Option[Extractor] = 
    extractors.find(_.outputRelation == name)

  def findRelationDependencies(name: String) : Set[String] = {
    findRelation(name).map(_.foreignKeys.filter(_.parentRelation != name)
      .map(_.parentRelation)).getOrElse(Nil)
      .flatMap(findRelationDependencies).toSet + name
  }

  def findExtractorDependencies(name: String) : Set[String] = {
    val extractorRelations = extractors.map(_.outputRelation).toSet
    val deps = extractors.find(_.name == name).map(_.outputRelation).map(findRelationDependencies).getOrElse(Set())
    deps.filter(x => extractorRelations.contains(x))
  }


  def databaseUrl : String = {
    s"jdbc:postgresql://${connection.host}:${connection.port}/${connection.db}"
  }

}

case class Settings(connection: Connection, relations: List[Relation], 
  etlTasks: List[EtlTask], extractors: List[Extractor], factors: List[FactorDesc]) 
  extends SettingsImpl


