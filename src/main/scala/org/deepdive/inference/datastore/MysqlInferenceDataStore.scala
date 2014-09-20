package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, FileWriter,
  StringWriter, Reader, FileReader, InputStream, InputStreamReader, FileInputStream}
import java.io.PrintWriter
import org.deepdive.calibration._
import org.deepdive.datastore.MysqlDataStore
import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.helpers.Helpers
import scala.sys.process._
import scala.collection.mutable.{ArrayBuffer, Set, SynchronizedBuffer}
import scala.io.Source
import org.apache.commons.io.FileUtils

/* Stores the factor graph and inference results in a postges database. */
trait MysqlInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  // Do not define inferenceDatastore here, define it in the class with this trait.
  // Since we have to pass parameters there. May need to refactor.
  
  class MysqlInferenceDataStore(val dbSettings : DbSettings) extends SQLInferenceDataStore with Logging {

    implicit lazy val connection = MysqlDataStore.borrowConnection()
    
    def ds = MysqlDataStore
    
    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
    
    
    def copyFileToTable(filePath: String, tableName: String) : Unit = {

      val srcFile = new File(filePath + ".text")

      val writebackCmd = "mysql " +
        Helpers.getOptionString(dbSettings) +
        " --silent -e " + "\"" +
        s"LOAD DATA LOCAL INFILE '${filePath}.text' " +
        s"INTO TABLE ${tableName} " +
        "FIELDS TERMINATED BY ' '" + "\""

      val tmpFile = File.createTempFile("copy_weight", ".sh")
      val writer = new PrintWriter(tmpFile)
      writer.println(s"${writebackCmd}")
      writer.close()
      executeCmd(tmpFile.getAbsolutePath())

//      executeCmd(writebackCmd)
    }
    /**
     * This function is called after sampler terminates. 
     * It just copies weights from the sampler outputs into database.
     */
    def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit = {
      copyFileToTable(weightsFile, WeightResultTable)
//      val deserializier = new ProtobufInferenceResultDeserializier()
//      val weightResultStr = deserializier.getWeights(weightsFile).map { w =>
//        s"${w.weightId}\t${w.value}"
//      }.mkString("\n")
//      
//      copyStringToTable(weightResultStr, WeightResultTable)
      
//      MysqlDataStore.copyBatchData(s"COPY ${WeightResultTable}(id, weight) FROM STDIN",
//        new java.io.StringReader(weightResultStr))
    }

    /**
     * This function is called after sampler terminates.
     * It just copies variables from the sampler outputs into database.
     */
    def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit = {
      copyFileToTable(variablesFile, VariableResultTable)

      //      val deserializier = new ProtobufInferenceResultDeserializier()
//      val variableResultStr = deserializier.getVariables(variablesFile).map { v =>
//        s"${v.variableId}\t${v.category}\t${v.expectation}"
//      }.mkString("\n")
//      
//      copyStringToTable(variableResultStr, VariableResultTable)
      
//      MysqlDataStore.copyBatchData(s"COPY ${VariableResultTable}(id, category, expectation) FROM STDIN",
//        new java.io.StringReader(variableResultStr))
    }

  }
}
