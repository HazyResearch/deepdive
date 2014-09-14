package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, FileWriter,
  StringWriter, Reader, FileReader, InputStream, InputStreamReader}
import org.deepdive.calibration._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.Logging
import org.deepdive.settings._
import scala.collection.mutable.{ArrayBuffer, Set, SynchronizedBuffer}
import scala.io.Source

/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  class PostgresInferenceDataStore(val dbSettings : DbSettings) extends SQLInferenceDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    def ds = PostgresDataStore

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
    
     def bulkCopyWeights(weightsFile: String) : Unit = {
      val deserializier = new ProtobufInferenceResultDeserializier()
      val weightResultStr = deserializier.getWeights(weightsFile).map { w =>
        s"${w.weightId}\t${w.value}"
      }.mkString("\n")
      PostgresDataStore.copyBatchData(s"COPY ${WeightResultTable}(id, weight) FROM STDIN",
        new java.io.StringReader(weightResultStr))
     }

    def bulkCopyVariables(variablesFile: String) : Unit = {
      val deserializier = new ProtobufInferenceResultDeserializier()
      val variableResultStr = deserializier.getVariables(variablesFile).map { v =>
        s"${v.variableId}\t${v.category}\t${v.expectation}"
      }.mkString("\n")
      PostgresDataStore.copyBatchData(s"COPY ${VariableResultTable}(id, category, expectation) FROM STDIN",
        new java.io.StringReader(variableResultStr))
    }
  }
}
