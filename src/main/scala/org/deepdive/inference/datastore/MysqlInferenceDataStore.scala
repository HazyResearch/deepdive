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
    
    /**
     * Internal utility to copy a file to a table. uses LOAD DATA LOCAL INFILE 
     * to retrieve the file in client-side rather than server-side. 
     */
    private def copyFileToTable(filePath: String, tableName: String) : Unit = {

      val srcFile = new File(filePath)

      val writebackCmd = "mysql " +
        Helpers.getOptionString(dbSettings) +
        " --silent -N -e " + "\"" +
        s"LOAD DATA LOCAL INFILE '${filePath}' " +
        s"INTO TABLE ${tableName} " +
        "FIELDS TERMINATED BY ' '" + "\""

      val tmpFile = File.createTempFile("copy_weight", ".sh")
      val writer = new PrintWriter(tmpFile)
      writer.println(s"${writebackCmd}")
      writer.close()
      Helpers.executeCmd(tmpFile.getAbsolutePath())
    }
    
    /**
     * This function is called after sampler terminates.
     * It just copies weights from the sampler outputs into database.
     */
    def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings): Unit = {
      copyFileToTable(weightsFile, WeightResultTable)
    }

    /**
     * This function is called after sampler terminates.
     * It just copies variables from the sampler outputs into database.
     */
    def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings): Unit = {
      copyFileToTable(variablesFile, VariableResultTable)
    }

    /**
     * Drop and create a sequence, based on database type.
     *
     * @see http://dev.mysql.com/doc/refman/5.0/en/user-variables.html
     * @see http://www.it-iss.com/mysql/mysql-renumber-field-values/
     */
    def createSequenceFunction(seqName: String): String = s"SET @${seqName} = -1;"

    /**
     * Get the next value of a sequence
     */
    def nextVal(seqName: String): String = s" @${seqName} := @${seqName} + 1 "

    /**
     * Cast an expression to a type
     */
    def cast(expr: Any, toType: String): String = 
      toType match {
        // convert text/varchar to char(N) where N is max length of given
        case "text" => s"convert(${expr.toString()}, char)"
        case "varchar" => s"convert(${expr.toString()}, char)"
        // in mysql, convert to unsigned guarantees bigint.
        // @see http://stackoverflow.com/questions/4660383/how-do-i-cast-a-type-to-a-bigint-in-mysql
        case "bigint" => s"convert(${expr.toString()}, unsigned)"
        case "int" => s"convert(${expr.toString()}, unsigned)"
        case "real" => s"${expr.toString()} + 0.0"
        // for others, try to convert as it is expressed.
        case _ => s"convert(${expr.toString()}, ${toType})"
      }
    
    /**
     * Concatinate multiple strings use "concat" function in mysql
     */
    def concat(list: Seq[String], delimiter: String): String = {
      list.length match {
        // return a SQL empty string if list is empty
        case 0 => "''" 
        case _ =>
        delimiter match {
          case null => s"concat(${list.mkString(", ")})"
          case "" => s"concat(${list.mkString(", ")})"
          case _ => s"concat(${list.mkString(s",'${delimiter}',")})"
        }
      }
    }

    /**
     * Given a string column name, Get a quoted version dependent on DB.
     *
     *          if psql, return "column"
     *          if mysql, return `column`
     */
    def quoteColumn(column: String): String = '`' + column + '`'
    
    def randomFunction: String = "RAND()"

    // ============== Datastore-specific queries to override ==============

    /**
     * Note that mysql cannot have nested views.
     * This utility creates a subquery for nesting views for mysql.
     * The view will be named to "NAME_sub".
     */
    private def createSubQueryForCalibrationViewMysql(name: String, bucketedView: String) =
      s"""CREATE OR REPLACE VIEW ${name}_sub1 AS 
        SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket;
      """

    override def createCalibrationViewBooleanSQL(name: String, bucketedView: String, columnName: String) = s"""
      ${createSubQueryForCalibrationViewMysql(name, bucketedView)}
      CREATE OR REPLACE VIEW ${name}_sub2 AS SELECT bucket, COUNT(*) AS num_correct from ${bucketedView} 
          WHERE ${columnName}=true GROUP BY bucket;
          
      CREATE OR REPLACE VIEW ${name}_sub3 AS SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView} 
          WHERE ${columnName}=false GROUP BY bucket;
          
      CREATE OR REPLACE VIEW ${name} AS
        SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
        ${name}_sub1 b1
        LEFT JOIN ${name}_sub2 b2 ON b1.bucket = b2.bucket
        LEFT JOIN ${name}_sub3 b3 ON b1.bucket = b3.bucket 
        ORDER BY b1.bucket ASC;
      """

    override def WRONGcreateCalibrationViewRealNumberSQL(name: String, bucketedView: String, columnName: String) = s"""
      ${createSubQueryForCalibrationViewMysql(name, bucketedView)}
    
      CREATE OR REPLACE VIEW ${name}_sub2 AS SELECT bucket, COUNT(*) AS num_correct from ${bucketedView} 
        WHERE ${columnName}=0.0 GROUP BY bucket;
        
      CREATE OR REPLACE VIEW ${name}_sub3 AS SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView} 
        WHERE ${columnName}=0.0 GROUP BY bucket;
    
      CREATE OR REPLACE VIEW ${name} AS
      SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
      ${name}_sub1 b1
      LEFT JOIN ${name}_sub2 b2 ON b1.bucket = b2.bucket
      LEFT JOIN ${name}_sub3 b3 ON b1.bucket = b3.bucket 
      ORDER BY b1.bucket ASC;
      """

    override def createCalibrationViewMultinomialSQL(name: String, bucketedView: String, columnName: String) = s"""
      ${createSubQueryForCalibrationViewMysql(name, bucketedView)}

      CREATE OR REPLACE VIEW ${name}_sub2 AS SELECT bucket, COUNT(*) AS num_correct from ${bucketedView} 
        WHERE ${columnName} = category GROUP BY bucket;
        
      CREATE OR REPLACE VIEW ${name}_sub3 AS SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView} 
        WHERE ${columnName} != category GROUP BY bucket;
      
      CREATE OR REPLACE VIEW ${name} AS
      SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
      ${name}_sub1 b1
      LEFT JOIN ${name}_sub2 b2 ON b1.bucket = b2.bucket
      LEFT JOIN ${name}_sub3 b3 ON b1.bucket = b3.bucket 
      ORDER BY b1.bucket ASC;
      """

    /**
     * Dumb: mysql cannot drop index "if exists"...
     * We use 4 statements to implement that.
     */
    private def dropIndexIfExistsMysql(indexName: String, tableName: String): String = {
      s"""
      set @exist := (select count(*) from information_schema.statistics 
        where table_name = '${tableName}' and index_name = '${indexName}');
      set @sqlstmt := if( @exist > 0, 'drop index ${indexName} on ${tableName}', 
        'select ''"index ${indexName}" do not exist, skipping''');
      PREPARE stmt FROM @sqlstmt;
      EXECUTE stmt;
    """
    }

    override def createInferenceResultIndiciesSQL = s"""
      ${dropIndexIfExistsMysql(s"${WeightResultTable}_idx", WeightResultTable)}
      ${dropIndexIfExistsMysql(s"${VariableResultTable}_idx", VariableResultTable)}
      CREATE INDEX ${WeightResultTable}_idx ON ${WeightResultTable} (weight);
      CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} (expectation);
      """

  }
}
