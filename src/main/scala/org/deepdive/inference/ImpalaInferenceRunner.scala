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
        ORDER BY b1.bucket ASC;
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
        ORDER BY b1.bucket ASC;
        """
  

    override def copyLastWeightsSQL = s"""
        DROP TABLE IF EXISTS ${lastWeightsTable};
        CREATE TABLE ${lastWeightsTable} AS
          SELECT X.*, Y.weight
        FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id;
        """

    override def createInferenceResultSQL = s"""
        DROP TABLE IF EXISTS ${VariableResultTable};
        CREATE TABLE ${VariableResultTable}(
          id bigint,
          category bigint,
          expectation double precision);
        """

    override def createInferenceResultWeightsSQL = s"""
        DROP TABLE IF EXISTS ${WeightResultTable};
        CREATE TABLE ${WeightResultTable}(
          id bigint primary key,
          weight double precision);
        """

  /**
   * Create a table of how LR features are supported by supervision examples
   */
  override def createFeatureStatsSupportTableSQL =
    s"""DROP TABLE IF EXISTS ${FeatureStatsSupportTable};

  CREATE TABLE ${FeatureStatsSupportTable}(
    description string,
    pos_examples bigint,
    neg_examples bigint,
    queries bigint);"""


  // assign variable holdout
  override def assignHoldout(schema: Map[String, _ <: VariableDataType], calibrationSettings: CalibrationSettings) {
    // variable holdout table - if user defined, execute once

    dataStore.dropAndCreateTable(VariablesHoldoutTable, "variable_id bigint")
    calibrationSettings.holdoutQuery match {
      case Some(query) => {
        log.info("Executing user supplied holdout query")
        execute(query)
      }
      case None => {
        log.info("There is no holdout query, will randomly generate holdout set")
         // randomly assign variable holdout
        schema.foreach { case(variable, dataType) =>
          val Array(relation, column) = variable.split('.')
          // This cannot be parsed in def randFunc for now.
          // assign holdout - randomly select from evidence variables of each variable table
          execute(s"""
            INSERT INTO ${VariablesHoldoutTable}
            SELECT id FROM ${relation}
            WHERE ${dataStore.randomFunction} < ${calibrationSettings.holdoutFraction} AND ${column} IS NOT NULL;
            """)
        }
      }
    }

    // variable observation table
    dataStore.dropAndCreateTable(VariablesObservationTable, "variable_id bigint")
    calibrationSettings.observationQuery match {
      case Some(query) => {
        log.info("Executing user supplied observation query")  
        execute(query)
      }
      case None => {
        log.info("There is no o query")
      }
    }

  }

  // generate cardinality tables for variables in the schema
  // cardinality tables is used to indicate the domains of the variables
  override def generateCardinalityTables(schema: Map[String, _ <: VariableDataType]) {
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      // note we use five-digit fixed-length representation here
      val cardinalityValues = dataType match {
        case BooleanType => "('00001')"
        case MultinomialType(x) => (0 to x-1).map (n => s"""('${"%05d".format(n)}')""").mkString(", ")
      }
      val cardinalityTableName = InferenceNamespace.getCardinalityTableName(relation, column)
      dataStore.dropAndCreateTable(cardinalityTableName, "cardinality string")
      execute(s"""
        INSERT INTO ${cardinalityTableName} VALUES ${cardinalityValues};
        """)
    }
  }

  // ground factor meta data
  override def groundFactorMeta(du: DataLoader, factorDescs: Seq[FactorDesc], dbSettings: DbSettings,
    groundingPath: String) {
    dataStore.dropAndCreateTable(FactorMetaTable, "name string, funcid int, sign string")

    // generate a string containing the signs (whether negated) of variables for each factor
    factorDescs.foreach { factorDesc =>
      val signString = factorDesc.func.variables.map(v => !v.isNegated).mkString(" ")
      val funcid = InferenceNamespace.getFactorFunctionTypeid(factorDesc.func.getClass.getSimpleName)
      execute(s"INSERT INTO ${FactorMetaTable} VALUES ('${factorDesc.name}', ${funcid}, '${signString}')")
    }

    // dump factor meta data
    val groundingDir = getFileNameFromPath(groundingPath)
    du.unload(InferenceNamespace.getFactorMetaFileName, 
      s"${groundingPath}/${InferenceNamespace.getFactorMetaFileName}", 
      dbSettings, s"SELECT * FROM ${FactorMetaTable}",
      groundingDir)
  }

  override def groundFactorsAndWeights(factorDescs: Seq[FactorDesc],
    calibrationSettings: CalibrationSettings, du: DataLoader,
    dbSettings: DbSettings, groundingPath: String,
    skipLearning: Boolean, weightTable: String) {
    val groundingDir = getFileNameFromPath(groundingPath)

    // save last weights
    if (skipLearning && weightTable.isEmpty()) {
      execute(copyLastWeightsSQL)
    }

    // weights table
    dataStore.dropAndCreateTable(WeightsTable, """id bigint, isfixed int, initvalue real, cardinality string, 
      description string""")
    
    // Create the feature stats table
    execute(createFeatureStatsSupportTableSQL)

    // weight and factor id
    // greenplum: use fast_seqassign postgres: use sequence
    var cweightid : Long = 0
    var factorid : Long = 0
    val weightidSequence = "dd_weight_sequence"
    val factoridSequence = "dd_factor_sequence"
    execute(dataStore.createSequenceFunction(weightidSequence));
    execute(dataStore.createSequenceFunction(factoridSequence));

    factorDescs.zipWithIndex.foreach { case (factorDesc, idx) =>
      // id columns
      val idcols = factorDesc.func.variables.map(v => 
        s""" ${dataStore.quoteColumn(s"${v.relation}.id")} """).mkString(", ")
      // Sen
      // val querytable = s"dd_query_${factorDesc.name}"
      // val weighttableForThisFactor = s"dd_weights_${factorDesc.name}"
      val querytable = InferenceNamespace.getQueryTableName(factorDesc.name)
      val weighttableForThisFactor = InferenceNamespace.getWeightTableName(factorDesc.name)

      val outfile = InferenceNamespace.getFactorFileName(factorDesc.name)

      val withIdQuery = s"CREATE TABLE ${querytable} AS SELECT t.*, ${factorid} + row_number() over(order by count(*)) FROM (${factorDesc.inputQuery}) t"
      execute(withIdQuery)

      // table of input query
      //dataStore.dropAndCreateTableAs(querytable, factorDesc.inputQuery)
      //execute(s"""ALTER TABLE ${querytable} ADD COLUMN id bigint;""")



      // handle factor id
      factorid += dataStore.assignIds(querytable.toLowerCase(), factorid, factoridSequence)

      // weight variable list
      val weightlist = factorDesc.weight.variables.map(v => 
        s""" ${dataStore.quoteColumn(v)} """).mkString(",")
      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val initvalue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }

      // for mysql, create indexes on weight variables of query tables.
      // for psql, this function is overwritten to do nothing.
      createIndexesForQueryTable(querytable, factorDesc.weight.variables)

      // generate weight description
      def generateWeightDesc(weightPrefix: String, weightVariables: Seq[String]) : String =
        dataStore.concat(weightVariables.map ( v => 
          s"""(CASE WHEN ${dataStore.quoteColumn(v)} IS NULL THEN '' ELSE ${dataStore.cast(dataStore.quoteColumn(v), "text")} END)""" ), 
          "-") // Delimiter '-' for concat
          match {
            case "" => s"'${weightPrefix}-' "
            // concatinate the "prefix-" with the weight values
            case x => dataStore.concat(Seq(s"'${weightPrefix}-' ", x), "")
      }
      val weightDesc = generateWeightDesc(factorDesc.weightPrefix, factorDesc.weight.variables)

      if (factorDesc.func.getClass.getSimpleName != "MultinomialFactorFunction") {

        // branch if weight variables present
        val hasWeightVariables = !(isFixed || weightlist == "")
        hasWeightVariables match {
            // create a table that only contains one row (one weight) 
            case false => dataStore.dropAndCreateTableAs(weighttableForThisFactor, 
              s"""SELECT ${dataStore.cast(isFixed, "int")} AS isfixed, ${dataStore.cast(initvalue, "float")} AS initvalue, 
                ${dataStore.cast(0, "bigint")} AS id;""")
            // create one weight for each different element in weightlist.
            case true => dataStore.dropAndCreateTableAs(weighttableForThisFactor,
              s"""SELECT ${weightlist}, ${dataStore.cast(isFixed, "int")} AS isfixed, 
              ${dataStore.cast(initvalue, "float")} AS initvalue, ${dataStore.cast(0, "bigint")} AS id
              FROM ${querytable}
              GROUP BY ${weightlist}""")
          }

          // handle weight id
          cweightid += dataStore.assignIds(weighttableForThisFactor.toLowerCase(), cweightid, weightidSequence)

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, description) 
            SELECT id, isfixed, initvalue, ${weightDesc} FROM ${weighttableForThisFactor};""")

          // check null weight (only if there are weight variables)
          if (hasWeightVariables) {
            val weightChecklist = factorDesc.weight.variables.map(v => s""" ${dataStore.quoteColumn(v)} IS NULL """).mkString("AND")
            issueQuery(s"SELECT COUNT(*) FROM ${querytable} WHERE ${weightChecklist}") { rs =>
              if (rs.getLong(1) > 0) {
                throw new RuntimeException("Weight variable has null values")
              }
            }
          }

          // dump factors
          val weightjoinlist = factorDesc.weight.variables.map(
            v => s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")
          // do not have join conditions if there are no weight variables, and t1 will only have 1 row
          val weightJoinCondition = hasWeightVariables match {
            case true => "WHERE " + factorDesc.weight.variables.map(
                v => s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")
            case false => ""
          }
          execute(dataStore.analyzeTable(querytable))
          execute(dataStore.analyzeTable(weighttableForThisFactor))
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
            s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
             FROM ${querytable} t0, ${weighttableForThisFactor} t1
             ${weightJoinCondition};""", groundingDir)

      } else if (factorDesc.func.getClass.getSimpleName == "MultinomialFactorFunction") {
        // TODO needs better code reuse
        // handle multinomial
        // generate cardinality table for each variable
        factorDesc.func.variables.zipWithIndex.foreach { case(v,idx) =>
          val cardinalityTableName = InferenceNamespace.getCardinalityInFactorTableName(
            factorDesc.weightPrefix, idx)
          dataStore.dropAndCreateTableAs(cardinalityTableName, s"""SELECT * FROM
            ${InferenceNamespace.getCardinalityTableName(v.headRelation, v.field)};""")
        }

        // cardinality values used in weight
        val cardinalityValues = factorDesc.func.variables.zipWithIndex.map { case(v,idx) => 
          s"""_c${idx}.cardinality"""
        }
        val cardinalityTables = factorDesc.func.variables.zipWithIndex.map { case(v,idx) =>
          s"${InferenceNamespace.getCardinalityInFactorTableName(factorDesc.weightPrefix, idx)} AS _c${idx}"
        }
        val cardinalityCmd = s"""${dataStore.concat(cardinalityValues,",")}"""

        // handle weights table
        // weight is fixed, or doesn't have weight variables
        if (isFixed || weightlist == ""){
          dataStore.dropAndCreateTableAs(weighttableForThisFactor, s"""
            SELECT ${dataStore.cast(isFixed, "int")} AS isfixed, ${initvalue} AS initvalue,
            ${cardinalityCmd} AS cardinality, ${cweightid} AS id
            FROM ${cardinalityTables.mkString(", ")}
            ORDER BY cardinality""")

          // handle weight id
          val count = dataStore.assignIds(weighttableForThisFactor.toLowerCase(), cweightid, weightidSequence)

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description) 
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${weighttableForThisFactor};""")

          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
            s"SELECT id AS factor_id, ${cweightid} AS weight_id, ${idcols} FROM ${querytable}",
            groundingDir)

          // increment weight id
          cweightid += count

        } else { // not fixed and has weight variables
          // temporary weight table for weights without a cross product with cardinality
          val weighttableForThisFactorTemp = s"dd_weight_${factorDesc.name}_temp"

          dataStore.dropAndCreateTableAs(weighttableForThisFactorTemp,
            s"""SELECT ${weightlist}, ${dataStore.cast(isFixed, "int")} AS isfixed,
            ${dataStore.cast(initvalue, "float")} AS initvalue
            FROM ${querytable}
            GROUP BY ${weightlist}""")
  
          // We need to create two tables -- one for a non-order'ed version
          // another for an ordered version. The reason that we cannot
          // do this with only one table is not fundemental -- it is just
          // a specific property of Greenplum to make it right
          dataStore.dropAndCreateTableAs(s"${weighttableForThisFactor}_unsorted",
            s"""SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality,
            ${dataStore.cast(0, "bigint")} AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0;""")
          
          execute(s"""
            INSERT INTO ${weighttableForThisFactor}_unsorted
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} as cardinality, 0 AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")}
            ORDER BY ${weightlist}, cardinality;""")
          
          dataStore.dropAndCreateTableAs(weighttableForThisFactor,
            s"""SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality,
            ${dataStore.cast(0, "bigint")} AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0""")

          execute(s"""
            INSERT INTO ${weighttableForThisFactor}
            SELECT * FROM ${weighttableForThisFactor}_unsorted
            ORDER BY ${weightlist}, cardinality;""")

          // handle weight id
          cweightid += dataStore.assignIds(weighttableForThisFactor.toLowerCase(), cweightid, weightidSequence)

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description) 
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${weighttableForThisFactor};""")

          // use weight id corresponding to cardinality 0 (like C array...)
          val cardinalityKey = factorDesc.func.variables.map(v => "00000").mkString(",")

          // dump factors
          // TODO we don't have enough code reuse here.
          val weightjoinlist = factorDesc.weight.variables.map(v => 
            s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")
          execute(dataStore.analyzeTable(querytable))
          execute(dataStore.analyzeTable(weighttableForThisFactor))
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
            s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
             FROM ${querytable} t0, ${weighttableForThisFactor} t1
             WHERE ${weightjoinlist} AND t1.cardinality = '${cardinalityKey}';""",
             groundingDir)
        }
      }
      // create feature stats for boolean LR
      createFeatureStats(factorDesc, querytable, weightlist, weightDesc)
    }

    if (skipLearning) {
      reuseWeights(weightTable)
    }

    // dump weights
    du.unload(InferenceNamespace.getWeightFileName,
      s"${groundingPath}/${InferenceNamespace.getWeightFileName}",dbSettings,
      s"SELECT id, isfixed, COALESCE(initvalue, 0) FROM ${WeightsTable}",
      groundingDir)
  }


  }
}
