package org.deepdive.settings

import com.typesafe.config._
import scala.collection.JavaConversions._
import scala.util.Try


object Settings {
  def loadFromConfig(config: Config) = SettingsParser.loadFromConfig(config)
}

trait SettingsImpl {

  def schemaSettings : SchemaSettings
  def extractionSettings : ExtractionSettings
  def inferenceSettings : InferenceSettings
  def calibrationSettings: CalibrationSettings
  def pipelineSettings : PipelineSettings

}

case class Settings(schemaSettings : SchemaSettings,
  extractionSettings: ExtractionSettings,
  inferenceSettings: InferenceSettings, 
  calibrationSettings: CalibrationSettings, 
  samplerSettings: SamplerSettings,
  pipelineSettings: PipelineSettings) extends SettingsImpl


