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
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection



/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  lazy val inferenceDataStore = new PostgresInferenceDataStore

  class PostgresInferenceDataStore extends SQLInferenceDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    def ds = PostgresDataStore

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
    
     def bulkCopyWeights(weightsFile: String) : Unit = {
        val connection = ds.borrowConnection()
        val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
        val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
        val copyManager = new CopyManager(pg_conn);
        val fileReader = new FileReader(weightsFile + ".text");
        copyManager.copyIn(s"COPY ${WeightResultTable}(id, weight) FROM STDIN DELIMITER ' '", fileReader );
      /*
      val deserializier = new ProtobufInferenceResultDeserializier()
      val weightResultStr = deserializier.getWeights(weightsFile).map { w =>
        s"${w.weightId}\t${w.value}"
      }.mkString("\n")
      PostgresDataStore.copyBatchData(s"COPY ${WeightResultTable}(id, weight) FROM STDIN",
        new java.io.StringReader(weightResultStr))
      */
     }

    def bulkCopyVariables(variablesFile: String) : Unit = {
        val connection = ds.borrowConnection()
        val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
        val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
        val copyManager = new CopyManager(pg_conn);
        val fileReader = new FileReader(variablesFile + ".text");
        copyManager.copyIn(s"COPY ${VariableResultTable}(id, category, expectation) FROM STDIN DELIMITER ' '", fileReader );

      /*
      val deserializier = new ProtobufInferenceResultDeserializier()
      val variableResultStr = deserializier.getVariables(variablesFile).map { v =>
        s"${v.variableId}\t${v.category}\t${v.expectation}"
      }.mkString("\n")
      PostgresDataStore.copyBatchData(s"COPY ${VariableResultTable}(id, category, expectation) FROM STDIN",
        new java.io.StringReader(variableResultStr))
      */
    }
  }
}
