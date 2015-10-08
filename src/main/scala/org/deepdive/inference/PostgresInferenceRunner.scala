package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import org.deepdive.calibration._
import org.deepdive.datastore._
import org.deepdive.inference._
import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.helpers.Helpers
import scala.collection.mutable.{ArrayBuffer, Set, SynchronizedBuffer}
import scala.io.Source
import java.io._

/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceRunnerComponent extends SQLInferenceRunnerComponent {

  class PostgresInferenceRunner(val dbSettings : DbSettings) extends SQLInferenceRunner with Logging with PostgresDataStoreComponent {

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)


    /**
     * weightsFile: location to the binary format. Assume "weightsFile.text" file exists.
     */
    def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit = {
      (new DataLoader).load(weightsFile, WeightResultTable, dbSettings, " ")
    }

    def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit = {
      (new DataLoader).load(variablesFile, VariableResultTable, dbSettings, " ")
    }

    /**
    * This query optimizes slow joins on certain DBMS (MySQL) by creating indexes
    * on the join condition column.
    */
    def createIndexForJoinOptimization(relation: String, column: String) = {}

    /**
     * For postgres, do not create indexes for query table
     */
    def createIndexesForQueryTable(queryTable: String, weightVariables: Seq[String]) = {}

    /**
     * This query is datastore-specific since it creates a view whose
     * SELECT contains a subquery in the FROM clause.
     * In Mysql the subqueries have to be created as views first.
     */
    def createCalibrationViewBooleanSQL(name: String, bucketedView: String, columnName: String) = s"""
        CREATE OR REPLACE VIEW ${name} AS
        SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
        (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
        LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView}
          WHERE ${columnName}=true GROUP BY bucket) b2 ON b1.bucket = b2.bucket
        LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView}
          WHERE ${columnName}=false GROUP BY bucket) b3 ON b1.bucket = b3.bucket
        ORDER BY b1.bucket ASC
        """

    /**
     * This query is datastore-specific since it creates a view whose
     * SELECT contains a subquery in the FROM clause.
     */
    def createCalibrationViewMultinomialSQL(name: String, bucketedView: String, columnName: String) = s"""
        CREATE OR REPLACE VIEW ${name} AS
        SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
        (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
        LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView}
          WHERE ${columnName} = category GROUP BY bucket) b2 ON b1.bucket = b2.bucket
        LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView}
          WHERE ${columnName} != category GROUP BY bucket) b3 ON b1.bucket = b3.bucket
        ORDER BY b1.bucket ASC
        """
  }
}
