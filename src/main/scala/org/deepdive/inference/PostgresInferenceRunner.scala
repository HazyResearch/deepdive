package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
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
  }
}
