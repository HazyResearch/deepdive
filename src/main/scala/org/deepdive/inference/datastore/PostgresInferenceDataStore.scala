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
import java.io._

/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  lazy val inferenceDataStore = new PostgresInferenceDataStore

  class PostgresInferenceDataStore extends SQLInferenceDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    def ds = PostgresDataStore

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
      

    def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit = {
     val dbname = dbSettings.dbname
     val pguser = dbSettings.user
     val pgport = dbSettings.port
     val pghost = dbSettings.host
     // TODO do not use password for now
     val dbnameStr = dbname match {
       case null => ""
       case _ => s" -d ${dbname} "
     }
     val pguserStr = pguser match {
       case null => ""
       case _ => s" -U ${pguser} "
     }
     val pgportStr = pgport match {
       case null => ""
       case _ => s" -p ${pgport} "
     }
     val pghostStr = pghost match {
       case null => ""
       case _ => s" -h ${pghost} "
     }
    
     val cmdfile = File.createTempFile(s"copy", ".sh")
     val writer = new PrintWriter(cmdfile)
     val copyStr = List("psql ", dbnameStr, pguserStr, pgportStr, pghostStr, " -c ", "\"\"\"", 
       """\COPY """, s"${WeightResultTable}(id, weight) FROM \'${weightsFile}.text\' DELIMITER ' ';", "\"\"\"").mkString("")
     log.info(copyStr)
     writer.println(copyStr)
     writer.close()
     executeCmd(cmdfile.getAbsolutePath())
    }
    
    def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit = {
     val dbname = dbSettings.dbname
     val pguser = dbSettings.user
     val pgport = dbSettings.port
     val pghost = dbSettings.host
     // TODO do not use password for now
     val dbnameStr = dbname match {
       case null => ""
       case _ => s" -d ${dbname} "
     }
     val pguserStr = pguser match {
       case null => ""
       case _ => s" -U ${pguser} "
     }
     val pgportStr = pgport match {
       case null => ""
       case _ => s" -p ${pgport} "
     }
     val pghostStr = pghost match {
       case null => ""
       case _ => s" -h ${pghost} "
     }
    
     val cmdfile = File.createTempFile(s"copy", ".sh")
     val writer = new PrintWriter(cmdfile)
     val copyStr = List("psql ", dbnameStr, pguserStr, pgportStr, pghostStr, " -c ", "\"\"\"", 
       """\COPY """, s"${VariableResultTable}(id, category, expectation) FROM \'${variablesFile}.text\' DELIMITER ' ';", "\"\"\"").mkString("")
     log.info(copyStr)
     writer.println(copyStr)
     writer.close()
     executeCmd(cmdfile.getAbsolutePath())
   }



    /*
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
    */
  }
}
