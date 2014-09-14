package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, FileWriter,
  StringWriter, Reader, FileReader, InputStream, InputStreamReader}
import java.io.PrintWriter
import org.deepdive.calibration._
import org.deepdive.datastore.MysqlDataStore
import org.deepdive.Logging
import org.deepdive.settings._
import scala.sys.process._
import scala.collection.mutable.{ArrayBuffer, Set, SynchronizedBuffer}
import scala.io.Source

/* Stores the factor graph and inference results in a postges database. */
trait MysqlInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  // Do not define inferenceDatastore here, define it in the class with this trait.
  // Since we have to pass parameters there. May need to refactor.
  
  class MysqlInferenceDataStore(val dbSettings : DbSettings) extends SQLInferenceDataStore with Logging {

    implicit lazy val connection = MysqlDataStore.borrowConnection()
    
    def ds = MysqlDataStore
    
    // Generate SQL query prefixes
    val dbname = dbSettings.dbname
    val dbuser = dbSettings.user
    val dbport = dbSettings.port
    val dbhost = dbSettings.host
    val dbpassword = dbSettings.password
    val dbnameStr = s" ${dbname} " // can also use -D but mysqlimport does not support -D
    val dbuserStr = dbuser match {
      case null => ""
      case _ => dbpassword match { // see if password is empty
        case null => s" -u ${dbuser} "
        case "" => s" -u ${dbuser} "
        case _ => s" -u ${dbuser} -p=${dbpassword}"
      }
    }
    val dbportStr = dbport match {
      case null => ""
      case _ => s" -P ${dbport} "
    }
    val dbhostStr = dbhost match {
      case null => ""
      case _ => s" -h ${dbhost} "
    }
  
    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
    
    def copyStringToTable(data: String, tableName: String) : Unit = {
      // Copy deserialized weights into a tsv file (dumb)
      
      // The base name of tmpfile must be same as table name.
      val weightsTmpFile = File.createTempFile(tableName + ".", ".tsv")
      try {
        val weightsWriter = new PrintWriter(weightsTmpFile)
        weightsWriter.print(data);
        weightsWriter.close()
        val writebackCmd = s"mysqlimport " + dbnameStr + dbuserStr + 
                        dbportStr + dbhostStr + s" ${weightsTmpFile.getAbsolutePath()}"
        executeCmd(writebackCmd)
      } finally {
        weightsTmpFile.delete();
      }

    }
    /**
     * This function is called after sampler terminates. 
     * It just copies weights from the sampler outputs into database.
     */
    def bulkCopyWeights(weightsFile: String) : Unit = {
      val deserializier = new ProtobufInferenceResultDeserializier()
      val weightResultStr = deserializier.getWeights(weightsFile).map { w =>
        s"${w.weightId}\t${w.value}"
      }.mkString("\n")
      
      copyStringToTable(weightResultStr, WeightResultTable)
      
//      MysqlDataStore.copyBatchData(s"COPY ${WeightResultTable}(id, weight) FROM STDIN",
//        new java.io.StringReader(weightResultStr))
    }

    /**
     * This function is called after sampler terminates.
     * It just copies variables from the sampler outputs into database.
     */
    def bulkCopyVariables(variablesFile: String) : Unit = {
      val deserializier = new ProtobufInferenceResultDeserializier()
      val variableResultStr = deserializier.getVariables(variablesFile).map { v =>
        s"${v.variableId}\t${v.category}\t${v.expectation}"
      }.mkString("\n")
      
      copyStringToTable(variableResultStr, VariableResultTable)
      
//      MysqlDataStore.copyBatchData(s"COPY ${VariableResultTable}(id, category, expectation) FROM STDIN",
//        new java.io.StringReader(variableResultStr))
    }

  }
}
