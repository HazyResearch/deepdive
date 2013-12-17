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
  
  def findExtractorDependencies(extractor: Extractor) : Set[String] = {
    extractor.dependencies.flatMap(findExtractor).flatMap(findExtractorDependencies)
  }

}

case class Settings(connection: Connection, relations: List[Relation], 
  etlTasks: List[EtlTask], extractors: List[Extractor], factors: List[FactorDesc], 
  calibrationSettings: CalibrationSettings) 
  extends SettingsImpl


