package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io._
import org.deepdive.datastore._
import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.helpers.Helpers
import scala.sys.process._
import scala.collection.mutable.{ArrayBuffer, Set, SynchronizedBuffer}
import scala.io.Source
import org.apache.commons.io.FileUtils

/* Stores the factor graph and inference results in a postges database. */
trait MysqlInferenceRunnerComponent extends SQLInferenceRunnerComponent {

  // Do not define inferenceDatastore here, define it in the class with this trait.
  // Since we have to pass parameters there. May need to refactor.

  class MysqlInferenceRunner(val dbSettings : DbSettings) extends SQLInferenceRunner with Logging with MysqlDataStoreComponent {

    // def ds : MysqlDataStore

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)

    /**
     * Internal utility to copy a file to a table. uses LOAD DATA INFILE
     * to retrieve the file in client-side rather than server-side.
     */
    private def copyFileToTable(filePath: String, tableName: String) : Unit = {

      val srcFile = new File(filePath)

      val writebackCmd = "mysql --local-infile" +
        Helpers.getOptionString(dbSettings) +
        " --silent -N -e " + "\"" +
        s"LOAD DATA LOCAL INFILE '${filePath}' " +
        s"INTO TABLE ${tableName} " +
        "FIELDS TERMINATED BY ' '" + "\""

      log.debug(s"Executing via file: ${writebackCmd}")
      val tmpFile = File.createTempFile("copy_weight", ".sh")
      val writer = new PrintWriter(tmpFile)
      writer.println(s"${writebackCmd}")
      writer.close()
      Helpers.executeCmd(tmpFile.getAbsolutePath())
      tmpFile.delete()
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

    // ============== Datastore-specific queries to override ==============

    /**
     * This query optimizes slow joins on certain DBMS (MySQL) by creating indexes
     * on the join condition column.
     */
    def createIndexForJoinOptimization(relation: String, column: String) = {
      val indexName = s"${relation}_${column}_idx"
      execute(s"""
        ${dropIndexIfExistsMysql(indexName, relation)};
        CREATE INDEX ${indexName} ON ${relation}(${column});
        """)
    }

    /**
     * Note that mysql cannot have nested views.
     * This utility creates a subquery for nesting views for mysql.
     * The view will be named to "NAME_sub".
     */
    private def createSubQueryForCalibrationViewMysql(name: String, bucketedView: String) =
      s"""CREATE OR REPLACE VIEW ${name}_sub1 AS
        SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket;
      """

    def createCalibrationViewBooleanSQL(name: String, bucketedView: String, columnName: String) = s"""
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

    def createCalibrationViewMultinomialSQL(name: String, bucketedView: String, columnName: String) = s"""
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
     *
     * Note: @exist will be NULL if the table do not exist, and "if" still branches into second.
     * Note: dbname must be provided to locate the index correctly
     */
    private def dropIndexIfExistsMysql(indexName: String, tableName: String): String = {
      s"""
      set @exist := (select count(*) from information_schema.statistics
        where table_name = '${tableName}' and index_name = '${indexName}'
        and TABLE_SCHEMA = '${dbSettings.dbname}');
      set @sqlstmt := if( @exist > 0, 'drop index ${indexName} on ${tableName}',
        'select ''"index ${indexName}" do not exist, skipping''');
      PREPARE stmt FROM @sqlstmt;
      EXECUTE stmt;
    """
    }

    /**
     * Inference view is very slow in MySQL so we create indexes on id,
     * and materialize this table for future queries (e.g. calibration)
     *
     * Note: "CREATE INDEX ${indexName}" clause can only handle non-text/blob type columns.
     */
    override def createInferenceViewSQL(relationName: String, columnName: String) = {
      val indexName = s"${relationName}_id_idx"
      s"""
      DROP TABLE IF EXISTS ${relationName}_${columnName}_inference CASCADE;

      ${dropIndexIfExistsMysql(indexName, relationName)}

      CREATE INDEX ${indexName} ON ${relationName}(id);

      CREATE TABLE ${relationName}_${columnName}_inference AS
      (SELECT ${relationName}.*, mir.category, mir.expectation FROM
      ${relationName}, ${VariableResultTable} mir
      WHERE ${relationName}.id = mir.id
      ORDER BY mir.expectation DESC);

    """
    }

    /**
     *  Create indexes for query table to speed up grounding. (this is useful for MySQL)
     *  Behavior may varies depending on different DBMS.
     */
    def createIndexesForQueryTable(queryTable: String, weightVariables: Seq[String]) = {
      log.debug("weight variables: ${factorDesc.weight.variables}")
      weightVariables.foreach( v => {
        val colType = checkColumnType(queryTable, v)
        if (colType.equals("text") || colType.equals("blob")) {
          // create a partial index
          execute(s"CREATE INDEX ${queryTable}_${v}_idx ON ${queryTable}(${v}(255))")
        } else {
          execute(s"CREATE INDEX ${queryTable}_${v}_idx ON ${queryTable}(${v})")
        }
      })
    }

  }
}
