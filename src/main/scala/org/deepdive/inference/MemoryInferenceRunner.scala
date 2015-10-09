package org.deepdive.inference

import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import java.io.{File, FileWriter, FileReader}
import org.deepdive.Logging
import org.deepdive.settings._
import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConversions._

trait MemoryInferenceRunnerComponent extends InferenceRunnerComponent{

  lazy val inferenceRunner = new MemoryInferenceRunner

  class MemoryInferenceRunner extends InferenceRunner with Logging {

    def init() = {
    }

    def groundFactorGraph(schema: Map[String, _ <: VariableDataType],
      factorDescs: Seq[FactorDesc], calibrationSettings: CalibrationSettings,
      skipLearning: Boolean, weightTable: String, dbSettings: DbSettings) : Unit = {

    }

    def BatchSize = None

    def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
      variableOutputFile: String, weightsOutputFile: String, dbSettings: DbSettings) : Unit = {
    }

  }

}
