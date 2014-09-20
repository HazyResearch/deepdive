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
import org.deepdive.helpers.Helpers

/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  class PostgresInferenceDataStore(val dbSettings : DbSettings) extends SQLInferenceDataStore with Logging {

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
     
     val cmdfile = File.createTempFile(s"copy", ".sh")
     val writer = new PrintWriter(cmdfile)
     val copyStr = List("psql ", Helpers.getOptionString(dbSettings), " -c ", "\"\"\"", 
       """\COPY """, s"${VariableResultTable}(id, category, expectation) FROM \'${variablesFile}.text\' DELIMITER ' ';", "\"\"\"").mkString("")
     log.info(copyStr)
     writer.println(copyStr)
     writer.close()
     executeCmd(cmdfile.getAbsolutePath())
   }

    /**
     * Drop and create a sequence, based on database type.
     */
    def createSequenceFunction(seqName: String): String =
      s"""DROP SEQUENCE IF EXISTS ${seqName} CASCADE;
          CREATE SEQUENCE ${seqName} MINVALUE -1 START 0;"""

    /**
     * Get the next value of a sequence
     */
    def nextVal(seqName: String): String =
      s""" nextval('${seqName}') """

    /**
     * Cast an expression to a type
     */
    def cast(expr: Any, toType: String): String =
      s"""${expr.toString()}::${toType}"""

    /**
     * Given a string column name, Get a quoted version dependent on DB.
     *          if psql, return "column"
     *          if mysql, return `column`
     */
    def quoteColumn(column: String): String =
      '"' + column + '"'
      
    def randomFunction: String = "RANDOM()"

  }
}
