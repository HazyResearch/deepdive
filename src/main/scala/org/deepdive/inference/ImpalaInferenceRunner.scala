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
trait ImpalaInferenceRunnerComponent extends SQLInferenceRunnerComponent {

  class ImpalaInferenceRunner(val dbSettings : DbSettings) extends SQLInferenceRunner with Logging with ImpalaDataStoreComponent {

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)

    override def init() : Unit = { }

    /**
     * weightsFile: location to the binary format. Assume "weightsFile.text" file exists.
     */
    def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit = {
      (new DataLoader).load(weightsFile, WeightResultTable, dbSettings, " ")
    }
    
    def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit = {
      (new DataLoader).load(variablesFile, VariableResultTable, dbSettings, " ")
    }

  override def createFactorQueryTableWithId(factorDesc:FactorDesc, startId:Long, sequenceName:String): Long = {
    val querytable = InferenceNamespace.getQueryTableName(factorDesc.name)
    
    // determine schema of factor query
    val columnNameTypes = dataStore.columnNameTypes(s"""SELECT * FROM (${factorDesc.inputQuery}) t LIMIT 0""")

    // now, we need to rename the columns as id0, v0, id1, v1 etc. because impala
    // does not support periods in column names of tables
    //val newColumnNames = new Array[String](columnNames.size)
    val superIdCols = super.getIdCols(factorDesc)
    val superValCols = super.getValCols(factorDesc)
    val newColumnNameTypes = for (i <- 0 until columnNameTypes.size) yield {
      var n = columnNameTypes(i)
      for (j <- 0 until superIdCols.size)
        if (columnNameTypes(i).equals(superIdCols(j)))
          n = ("id" + j, n._2)
      for (j <- 0 until superValCols.size)
        if (columnNameTypes(i).equals(superValCols(j)))
          n = ("v" + j, n._2)
      n
    }
    val coldefstr = newColumnNameTypes.map { case (n,t) => s"$n $t" }.mkString(", ")

    val withIdQuery = s"""
      DROP TABLE IF EXISTS ${querytable};
      CREATE TABLE ${querytable} (${coldefstr}, id bigint);
        INSERT INTO ${querytable}
          SELECT t.*, cast(${startId} -2 + row_sequence() as bigint) as id
          FROM (${factorDesc.inputQuery}) t;
      """

    dataStore.executeSqlQueries(withIdQuery)
    var count : Long = 0
    dataStore.executeSqlQueryWithCallback(s"""SELECT COUNT(*) FROM ${querytable};""") { rs =>
      count = rs.getLong(1)
    }
    count
  }

  override def getIdCols(factorDesc:FactorDesc) =
      factorDesc.func.variables.zipWithIndex.map { case (v,i) => s"id$i" }

  override def getValCols(factorDesc:FactorDesc) =
    factorDesc.func.variables.zipWithIndex.map { case (v,i) => s"v$i" }
  }
}
