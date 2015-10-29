/**
 * Ground the factor graph to file
 *
 * Using the schema and inference rules defined in application.conf, construct factor
 * graph files.
 * Input: variable tables, factor descriptions, holdout configuration, (feature tables)
 * Output: factor graph files: variables, factors, edges, weights, meta that the
 * sampler can read. Refer to deepdive.stanford.edu for more details of the output format.
 *
 * The workflow is defined in groundFactorGraph. Table and file names are defined in
 * InferenceNamespace
 *
 * We might want to promulgate a new coding standard: no direct SQL queries without a function wrapper,
 * into which parameters are passed. The goal in doing that would be to enforce type checking by the compiler.
 * As it stands, if there's a type clash you don't find out until you try to run the query.
 *
 * DROP TABLE is such a toxic operation. One SHOULD call dataStore.dropAndCreateTable or
 * dataStore.dropAndCreateTableAs in order to drop and create a table. These method will
 * ensure we are only dropping tables inside the DeepDive namespace.
 */
package org.deepdive.inference

import java.io.{File, PrintWriter}
import org.deepdive.calibration._
import org.deepdive.datastore.JdbcDataStore
import org.deepdive.datastore.DataLoader
import org.deepdive.Logging
import org.deepdive.Context
import org.deepdive.helpers.Helpers
import org.deepdive.helpers.Helpers.{Psql, Mysql}
import org.deepdive.settings._
import play.api.libs.json._
import scalikejdbc._
import scala.util.matching._
import scala.io.Source
import scala.util.Random
import scala.sys.process._
import scala.util.{Try, Success, Failure}


/* Stores the factor graph and inference results. */
trait SQLInferenceRunnerComponent extends InferenceRunnerComponent {

  def inferenceRunner : SQLInferenceRunner

}


trait SQLInferenceRunner extends InferenceRunner with Logging {

  def dataStore : JdbcDataStore

  /* Internal Table names */
  def WeightsTable = "dd_graph_weights"
  def lastWeightsTable = "dd_graph_last_weights"
  def FactorsTable = "dd_graph_factors"
  def VariablesTable = "dd_graph_variables"
  def VariablesMapTable = "dd_graph_variables_map"
  def WeightResultTable = "dd_inference_result_weights"
  def VariablesHoldoutTable = "dd_graph_variables_holdout"
  def VariableResultTable = "dd_inference_result_variables"
  def MappedInferenceResultView = "dd_mapped_inference_result"
  def IdSequence = "dd_variable_sequence"
  def FactorMetaTable = "dd_graph_factormeta"
  def VariablesObservationTable = "dd_graph_variables_observation"
  def LearnedWeightsTable = "dd_inference_result_weights_mapping"

  /**
   * execute one or multiple SQL queries
   */
  def execute(sql: String) = {
    dataStore.executeSqlQueries(sql)
  }

  /**
   * Issues a single SQL query that can return results, and perform {@code op}
   * as callback function
   */
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {
    dataStore.executeSqlQueryWithCallback(sql)(op)
  }

  def copyLastWeightsSQL = {
   val distribution = if (dataStore.isUsingPostgresXL) "DISTRIBUTE BY REPLICATION" else ""
   s"""
    DROP TABLE IF EXISTS ${lastWeightsTable} CASCADE;
    CREATE ${dataStore.unlogged} TABLE ${lastWeightsTable} ${distribution} AS
      SELECT X.*, Y.weight
      FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id;
  """
 }

  def createInferenceResultSQL = s"""
    DROP TABLE IF EXISTS ${VariableResultTable} CASCADE;
    CREATE ${dataStore.unlogged} TABLE ${VariableResultTable}(
      id bigint,
      category bigint,
      expectation double precision);
  """

  def createInferenceResultWeightsSQL = s"""
    DROP TABLE IF EXISTS ${WeightResultTable} CASCADE;
    CREATE ${dataStore.unlogged} TABLE ${WeightResultTable}(
      id bigint primary key,
      weight double precision);
  """

  def createInferenceViewSQL(relationName: String, columnName: String) = s"""
    CREATE OR REPLACE VIEW ${relationName}_${columnName}_inference AS
    (SELECT ${relationName}.*, mir.category, mir.expectation FROM
    ${relationName}, ${VariableResultTable} mir
    WHERE ${relationName}.id = mir.id
    ORDER BY mir.expectation DESC);
  """

  def createBucketedCalibrationViewSQL(name: String, inferenceViewName: String, buckets: List[Bucket]) = {
    val bucketCaseStatement = buckets.zipWithIndex.map { case(bucket, index) =>
      s"WHEN expectation >= ${bucket.from} AND expectation <= ${bucket.to} THEN ${index}"
    }.mkString("\n")
    s"""CREATE OR REPLACE VIEW ${name} AS
      SELECT ${inferenceViewName}.*, CASE ${bucketCaseStatement} END bucket
      FROM ${inferenceViewName} ORDER BY bucket ASC;"""
  }

  /**
   *  Create indexes for query table to speed up grounding. (this is useful for MySQL)
   *  Behavior may varies depending on different DBMS.
   */
  def createIndexesForQueryTable(queryTable: String, weightVariables: Seq[String]) : Unit

  // ========= Datastore specific queries  ============

  /**
   * This query optimizes slow joins on certain DBMS (MySQL) by creating indexes
   * on the join condition column.
   */
  def createIndexForJoinOptimization(relation: String, column: String) : Unit

  /**
   * This query is datastore-specific since it creates a view whose
   * SELECT contains a subquery in the FROM clause.
   * In Mysql the subqueries have to be created as views first.
   */
  def createCalibrationViewBooleanSQL(name: String, bucketedView: String, columnName: String) : String

  /**
   * This query is datastore-specific since it creates a view whose
   * SELECT contains a subquery in the FROM clause.
   */
  def createCalibrationViewMultinomialSQL(name: String, bucketedView: String, columnName: String) : String

  // end data store specific query

  def selectCalibrationDataSQL(name: String) = s"""
    SELECT bucket as "bucket", num_variables AS "num_variables",
      num_correct AS "num_correct", num_incorrect AS "num_incorrect"
    FROM ${name};
  """

  def createMappedWeightsViewSQL = s"""
    CREATE OR REPLACE VIEW ${LearnedWeightsTable} AS
    SELECT ${WeightsTable}.*, ${WeightResultTable}.weight FROM
    ${WeightsTable} JOIN ${WeightResultTable} ON ${WeightsTable}.id = ${WeightResultTable}.id
    ORDER BY abs(weight) DESC;

    CREATE OR REPLACE VIEW ${VariableResultTable}_mapped_weights AS
    SELECT * FROM ${LearnedWeightsTable}
    ORDER BY abs(weight) DESC;
  """

  def init() : Unit = {
  }

  // given a path, get file/folder name
  // e.g., /some/path/to/folder -> folder
  def getFileNameFromPath(path: String) : String = {
    return new java.io.File(path).getName()
  }

  /**
   * Returns a column type. Applicable for all DBMSs.
   */
  def checkColumnType(table: String, column: String): String = {
    var colType = ""
    issueQuery(s"select data_type from information_schema.columns " +
        s"where table_name='${table}' and column_name='${column}';") { rs =>
        colType = rs.getString(1)
      }
    log.debug(s"Column type for ${table}: ${colType}")
    return colType
  }

  // assign variable id - sequential and unique
  def assignVariablesIds(schema: Map[String, _ <: VariableDataType], dbSettings: DbSettings) {
    // fast sequential id assign function
    dataStore.createSpecialUDFs()
    execute(dataStore.createSequenceFunction(IdSequence))
    dbSettings.incrementalMode match {
      case IncrementalMode.INCREMENTAL => assignVariableIdsIncremental(schema, dbSettings)
      case _ => {
        var idoffset : Long = 0
        schema.foreach { case(variable, dataType) =>
          val Array(relation, column) = variable.split('.')
          idoffset += dataStore.assignIds(relation, idoffset, IdSequence)
        }
      }
    }
  }

  // assign variable ids for incremental
  def assignVariableIdsIncremental(schema: Map[String, _ <: VariableDataType], dbSettings: DbSettings) {
    var idoffset : Long = 0
    issueQuery(s""" SELECT num_variables FROM ${InferenceNamespace.getIncrementalMetaTableName()}""") {
      rs => idoffset = rs.getLong(1)
      execute(s"ALTER SEQUENCE ${IdSequence} RESTART ${idoffset}")
    }

    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      val baseRelation = InferenceNamespace.getBaseTableName(relation)
      val key = dbSettings.keyMap(relation)
      val keyJoinlist = key.map(v => s""" t0.${v} = t1.${v} """).mkString("AND")
      val tmpTable = s"dd_inc_${relation}"

      // Assign Id based on original relation table otherwise assign new Id
      execute(s"""UPDATE ${relation} SET id = NULL""")
      execute(s"""UPDATE ${relation} AS t0 SET id = t1.id
        FROM ${baseRelation} t1
        WHERE ${keyJoinlist}""")
      dataStore.dropAndCreateTableAs(tmpTable, s"""
        SELECT ${key.mkString(", ")}, ${column}, id
        FROM ${relation}
        WHERE id is NULL; """)
      idoffset += dataStore.assignIds(tmpTable.toLowerCase(), idoffset, IdSequence)
      execute(s"""UPDATE ${relation} AS t0 SET id = t1.id
        FROM ${tmpTable} t1
        WHERE ${keyJoinlist}""")
      execute(s"""DROP TABLE ${tmpTable}""")
    }
  }

  // assign variable holdout
  def assignHoldout(schema: Map[String, _ <: VariableDataType], calibrationSettings: CalibrationSettings) {
    // variable holdout table - if user defined, execute once
    dataStore.dropAndCreateTable(VariablesHoldoutTable, "variable_id bigint primary key")
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
    dataStore.dropAndCreateTable(VariablesObservationTable, "variable_id bigint primary key")
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
  def generateCardinalityTables(schema: Map[String, _ <: VariableDataType]) {
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      // note we use five-digit fixed-length representation here
      val cardinalityValues = dataType match {
        case BooleanType => "('00001')"
        case MultinomialType(x) => (0 to x-1).map (n => s"""('${"%05d".format(n)}')""").mkString(", ")
      }
      val cardinalityTableName = InferenceNamespace.getCardinalityTableName(relation, column)
      dataStore.dropAndCreateTable(cardinalityTableName, "cardinality text")
      execute(s"""
        INSERT INTO ${cardinalityTableName} VALUES ${cardinalityValues};
        """)
    }
  }

  // ground variables
  def groundVariables(schema: Map[String, _ <: VariableDataType], du: DataLoader,
      dbSettings: DbSettings, groundingPath: String) {

    schema.foreach { case(variable, dataType) =>
      var Array(relation, column) = variable.split('.')

      val variableDataType = InferenceNamespace.getVariableDataTypeId(dataType)

      // cardinality (domain size) of the variable
      // boolean: 2
      // multinomial: as user defined
      val cardinality = dataType match {
        case BooleanType => 2
        case MultinomialType(x) => x.toInt
      }

      // Create a table to denote variable type - query, evidence, observation
      // variable table join with holdout table
      // - a variable is an evidence if it has initial value and it is not holdout
      val variableTypeColumn = "__dd_variable_type__"

      // IF:
      //  in observation table, in evidence table => Observation 2
      //  in holdout table => Query 1
      //  not in observation table, not in holdout table, in evidence table => Evidence 1
      //  else => Query 1
      val variableTypeTable = InferenceNamespace.getVariableTypeTableName(relation)
      dataStore.dropAndCreateTableAs(variableTypeTable,
        s"""SELECT t0.id, CASE WHEN t2.variable_id IS NOT NULL AND ${column} IS NOT NULL THEN 2
                           WHEN t1.variable_id IS NOT NULL THEN 0
                           WHEN ${column} IS NOT NULL THEN 1
                           ELSE 0
                      END as ${variableTypeColumn}
        FROM ${relation} t0 LEFT OUTER JOIN ${VariablesHoldoutTable} t1 ON t0.id=t1.variable_id
        LEFT OUTER JOIN ${VariablesObservationTable} t2 ON t0.id=t2.variable_id""")

      // dump variables
      val initvalueCast = dataStore.cast(dataStore.cast(column, "int"), "float")
      // Sen
      // du.unload(s"dd_variables_${relation}", s"${groundingPath}/dd_variables_${relation}",
      val groundingDir = getFileNameFromPath(groundingPath)

      val incCondition = dbSettings.incrementalMode match {
        case IncrementalMode.INCREMENTAL => {
          var numVariablesMat : Long = 0
          issueQuery(s""" SELECT num_variables FROM ${InferenceNamespace.getIncrementalMetaTableName()}""") {
            rs => numVariablesMat = rs.getLong(1)
          }
          s"""AND (t0.id >= ${numVariablesMat})"""
        }
        case _ => ""
      }
      du.unload(InferenceNamespace.getVariableFileName(relation),
        s"${groundingPath}/${InferenceNamespace.getVariableFileName(relation)}",
        dbSettings,
        s"""SELECT t0.id, t1.${variableTypeColumn},
        CASE WHEN t1.${variableTypeColumn} = 0 THEN 0 ELSE ${initvalueCast} END AS initvalue,
        ${variableDataType} AS type, ${cardinality} AS cardinality
        FROM ${relation} t0, ${variableTypeTable} t1
        WHERE t0.id=t1.id ${incCondition}
        """, groundingDir)
    }

    // maintain incremental meta data
    dbSettings.incrementalMode match {
      case IncrementalMode.MATERIALIZATION => {
        var numVariables : Long = 0
        schema.foreach { case(variable, dataType) =>
          var Array(relation, column) = variable.split('.')
          issueQuery(s"SELECT COUNT(*) FROM ${relation}") { rs =>
            numVariables += rs.getLong(1)
          }
        }
        execute(s"""UPDATE ${InferenceNamespace.getIncrementalMetaTableName()}
          SET num_variables = ${numVariables}; """)
      }
      case _ =>
    }
  }

  // ground factor meta data
  def groundFactorMeta(du: DataLoader, factorDescs: Seq[FactorDesc], dbSettings: DbSettings,
    groundingPath: String) {
    dataStore.dropAndCreateTable(FactorMetaTable, "name text, funcid int, sign text")

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

  // convert grounding file format to be compatible with sampler
  // for more information about format, please refer to deepdive's website
  def convertGroundingFormat(groundingPath: String) {
    log.info("Converting grounding file format...")
    // TODO: this python script is dangerous and ugly. It changes too many states!
    val cmd = Seq("sh", "-c", s"cd ${groundingPath} && tobinary.py . format_converter ${Context.outputDir}")
    log.debug("Executing: " + cmd)
    val exitValue = cmd!(ProcessLogger(
      out => log.info(out),
      err => log.info(err)
    ))

    exitValue match {
      case 0 =>
      case _ => throw new RuntimeException("Converting format failed.")
    }
  }

  def generateAcitve(groundingPath: String) {
    val cmd = s"active.sh"
    log.debug("Executing: " + cmd)
    val exitValue = cmd!(ProcessLogger(
      out => log.info(out),
      err => log.info(err)
    ))
    exitValue match {
      case 0 =>
      case _ => throw new RuntimeException("Generating active variables/factors/weights failed.")
    }
  }

  def createWeightsTable() {
    if (dataStore.isUsingPostgresXL) {
       val name = WeightsTable
       val schema = """id bigint, isfixed int, initvalue real, cardinality text,
        description text"""
       dataStore.checkTableNamespace(name)
       dataStore.executeSqlQueries(s"""DROP TABLE IF EXISTS ${name} CASCADE;""")
       dataStore.executeSqlQueries(s"""CREATE ${dataStore.unlogged} TABLE ${name} (${schema}) DISTRIBUTE BY REPLICATION;""")
    } else {
      dataStore.dropAndCreateTable(WeightsTable, """id bigint, isfixed int, initvalue real, cardinality text,
        description text""")
    }
  }

  // delete the tuples that have duplicates in the base table
  def deleteDuplicatesFromDelta(relation: String, joinCondition: String) {
    // this is achieved by creating a new table using outer join
    // and replace the original one
    val baseRelation = InferenceNamespace.getBaseTableName(relation)
    val tmpTable = s"dd_inc_${relation}"
    execute(s"""CREATE TABLE ${tmpTable} AS
      SELECT t0.*
      FROM ${relation} t0 LEFT OUTER JOIN ${baseRelation} t1
      ON ${joinCondition}
      WHERE t1.id IS NULL;
      """)
    execute(s"""TRUNCATE TABLE ${relation}""")
    execute(s"""INSERT INTO ${relation} SELECT * FROM ${tmpTable}""")
    execute(s"""DROP TABLE ${tmpTable}""")
  }

  def groundFactorsAndWeights(factorDescs: Seq[FactorDesc],
    calibrationSettings: CalibrationSettings, du: DataLoader,
    dbSettings: DbSettings, groundingPath: String,
    skipLearning: Boolean, weightTable: String) {
    val groundingDir = getFileNameFromPath(groundingPath)

    // save last weights
    if (skipLearning && weightTable.isEmpty()) {
      execute(copyLastWeightsSQL)
    }

    // weights table
    createWeightsTable()

    // weight and factor id
    // greenplum: use fast_seqassign postgres: use sequence
    var cweightid : Long = 0
    var factorid : Long = 0
    val weightidSequence = "dd_weight_sequence"
    val factoridSequence = "dd_factor_sequence"
    execute(dataStore.createSequenceFunction(weightidSequence));
    execute(dataStore.createSequenceFunction(factoridSequence));

    dbSettings.incrementalMode match {
      case IncrementalMode.INCREMENTAL =>  {
        issueQuery(s""" SELECT num_weights FROM ${InferenceNamespace.getIncrementalMetaTableName()}""") { rs =>
          cweightid = rs.getLong(1)
        }
        execute(s"ALTER SEQUENCE ${weightidSequence} RESTART ${cweightid}")
        issueQuery(s""" SELECT num_factors FROM ${InferenceNamespace.getIncrementalMetaTableName()}""") { rs =>
          factorid = rs.getLong(1)
        }
        execute(s"ALTER SEQUENCE ${factoridSequence} RESTART ${factorid}")
      }
      case _ =>
    }

    // weights table
    dataStore.dropAndCreateTable(WeightsTable, """id bigint, isfixed int, initvalue real, cardinality text,
      description text""")

    var factorDescsCopy : Seq[FactorDesc] = Seq[FactorDesc]()
    factorDescsCopy = factorDescs

    factorDescsCopy.zipWithIndex.foreach { case (factorDesc, idx) =>
      // id columns
      val idcols = factorDesc.func.variables.map(v =>
        s""" ${dataStore.quoteColumn(s"${v.relation}.id")} """).mkString(", ")

      // val filedcols = factorDesc.func.variables.map(v =>
      //     s""" every(${dataStore.quoteColumn(s"${v.relation}.${v.field}")}) """).mkString(", ")

      val weightcols = factorDesc.weight.variables.map(v =>
          s""" ${dataStore.quoteColumn(v)} """).mkString(", ")

      val selectcols = Seq(idcols, weightcols).mkString(", ")
      val condcols = Seq(idcols, weightcols).mkString(", ")

      // Sen
      // val querytable = s"dd_query_${factorDesc.name}"
      // val weighttableForThisFactor = s"dd_weights_${factorDesc.name}"
      val querytable = InferenceNamespace.getQueryTableName(factorDesc.name)
      val weighttableForThisFactor = InferenceNamespace.getWeightTableName(factorDesc.name)

      val outfile = InferenceNamespace.getFactorFileName(factorDesc.name)

      // Ground new factor table based on input query
      dataStore.dropAndCreateTableAs(querytable, factorDesc.inputQuery)


      // Assign Id for new factor table, reuse Id if factor already exists other wise assign new Id

      execute(s"""ALTER TABLE ${querytable} ADD COLUMN id bigint;""")

      // for incremental mode, delete duplicates (tuples that exist in base table) from delta table
      dbSettings.incrementalMode match {
        case IncrementalMode.INCREMENTAL => {
          // Create new save
          val baseFactorTable = InferenceNamespace.getBaseTableName(querytable)
          // if adding new inference rule, the original rule does not exist
          val exists = dataStore.existsTable(baseFactorTable)
          if (exists) {
            val factorJoinlist = factorDesc.func.variables.map(
              v => s""" t0.${dataStore.quoteColumn(s"${v.relation}.id")}
                |= t1.${dataStore.quoteColumn(s"${InferenceNamespace.getBaseTableName(v.relation)}.id")}
                |""".stripMargin.replaceAll("\n", " ")).mkString("AND")
            val weightJoinlist = factorDesc.weight.variables.map(v => {
              s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)}"""
            }).mkString("AND")
            val joinList = Seq(factorJoinlist, weightJoinlist).mkString(" AND ")

            // delete the tuples that have duplicates in the base table
            deleteDuplicatesFromDelta(querytable, joinList)
          }
        }
        case _ => {
        }
      }

      // handle factor id
      factorid += dataStore.assignIds(querytable.toLowerCase(), factorid, factoridSequence)

      // maintain incremental meta data
      dbSettings.incrementalMode match {
        case IncrementalMode.MATERIALIZATION => {
          var numFactors : Long = 0
          issueQuery(s"SELECT COUNT(*) FROM ${querytable}") { rs =>
            numFactors += rs.getLong(1)
          }
          execute(s"""UPDATE ${InferenceNamespace.getIncrementalMetaTableName()}
            SET num_factors = ${numFactors};""")
        }
        case _ =>
      }

      // weight variable list
      val weightlist = factorDesc.weight.variables.map(v =>
        s""" ${dataStore.quoteColumn(v)} """).mkString(",")
      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val initvalue = factorDesc.weight match {
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }

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

      val weightJoinlist = factorDesc.weight.variables.map(
        v => s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")

      // assign weight id, handle both normal case and incremental case
      def handleWeightIds(orderbyClause: String, isMultinomial: Boolean) = {
        // assign weight id for incremental, use previous weight id if we have seen that weight before,
        // otherwise, assign new id
        dbSettings.incrementalMode match {
          case IncrementalMode.INCREMENTAL => {
            val lastWeightsTableForThisFactor = InferenceNamespace.getBaseTableName(weighttableForThisFactor)
            val exists = dataStore.existsTable(lastWeightsTableForThisFactor)
            if (!exists) {
              assignWeightIds(weighttableForThisFactor, orderbyClause)
            } else {
              val tmpTable = s"${weighttableForThisFactor}_inc"
              val weightJoinlistInc = List(weightJoinlist)
              val cardinalityCond = if (isMultinomial) List("t0.cardinality = t1.cardinality") else List("")
              val weightConditionInc = (weightJoinlistInc ++ cardinalityCond).filter(_ != "").mkString("WHERE ", " AND ", "")
              execute(s"""UPDATE ${weighttableForThisFactor} AS t0 SET id = t1.id
                FROM ${lastWeightsTableForThisFactor} t1
                ${weightConditionInc}""")
              dataStore.dropAndCreateTableAs(tmpTable, s"SELECT * FROM ${weighttableForThisFactor} WHERE id = -1")
              execute(s"ALTER SEQUENCE ${weightidSequence} RESTART ${cweightid}")
              val count = assignWeightIds(tmpTable, orderbyClause)

              val weightCondition = (List(weightJoinlist) ++ cardinalityCond).filter(_ != "").mkString("WHERE ", " AND ", "")
              execute(s"""UPDATE ${weighttableForThisFactor} AS t0 SET id = t1.id
                FROM ${tmpTable} t1
                ${weightCondition}""")
              count
            }
          }
          case _ => {
            // handle weight id
            assignWeightIds(weighttableForThisFactor, orderbyClause)
          }
        }
      }

      // assign weight id
      // TODO this function is branching for PostgresXL, we need to get rid of the branching
      def assignWeightIds(table: String, orderbyClause: String) = {
        if (dataStore.isUsingPostgresXL)
          dataStore.assignIdsOrdered(table.toLowerCase(), cweightid, weightidSequence, orderbyClause)
        else
          dataStore.assignIds(table.toLowerCase(), cweightid, weightidSequence)
      }

      // update incremental weight meta data
      def updateIncWeightMeta() {
        dbSettings.incrementalMode match {
          case IncrementalMode.MATERIALIZATION => {
            var numWeights : Long = 0
            issueQuery(s"SELECT COUNT(*) FROM ${weighttableForThisFactor}") { rs =>
              numWeights += rs.getLong(1)
            }
            execute(s"""UPDATE ${InferenceNamespace.getIncrementalMetaTableName()}
              SET num_weights = ${numWeights}""")
          }
          case _ =>
        }
      }

      // condition for incremental mode
      val incCondition = dbSettings.incrementalMode match {
        case IncrementalMode.INCREMENTAL => {
          var numWeightsMat : Long = 0
          issueQuery(s""" SELECT num_weights FROM ${InferenceNamespace.getIncrementalMetaTableName()}""") {
            rs => numWeightsMat = rs.getLong(1)
          }
          s"""WHERE id >= ${numWeightsMat}"""
        }
        case _ => ""
      }

      if (factorDesc.func.getClass.getSimpleName != "MultinomialFactorFunction") {

        // branch if weight variables present
        val hasWeightVariables = !(isFixed || weightlist == "")
        hasWeightVariables match {
          // create a table that only contains one row (one weight)
          case false => dataStore.dropAndCreateTableAs(weighttableForThisFactor,
            s"""SELECT ${dataStore.cast(isFixed, "int")} AS isfixed, ${dataStore.cast(initvalue, "float")} AS initvalue,
              ${dataStore.cast(-1, "bigint")} AS id;""")
          // create one weight for each different element in weightlist.
          case true => dataStore.dropAndCreateTableAs(weighttableForThisFactor,
            s"""SELECT ${weightlist}, ${dataStore.cast(isFixed, "int")} AS isfixed,
            ${dataStore.cast(initvalue, "float")} AS initvalue, ${dataStore.cast(-1, "bigint")} AS id
            FROM ${querytable}
            GROUP BY ${weightlist}""")
        }

        // assign weight id for incremental, use previous weight id if we have seen that weight before,
        // otherwise, assign new id
        cweightid += handleWeightIds("", false)

        // maintain incremental meta data
        updateIncWeightMeta()

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
        // do not have join conditions if there are no weight variables, and t1 will only have 1 row
        val weightJoinCondition = hasWeightVariables match {
          case true => "WHERE " + factorDesc.weight.variables.map(
              v => s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")
          case false => ""
        }
        execute(dataStore.analyzeTable(querytable))
        execute(dataStore.analyzeTable(weighttableForThisFactor))

        // do not need DISTINCT in this sql, since for every tuple in the querytable,
        // there is only one matching tuple in the weighttableForThisFactor
        du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
          s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
           FROM ${querytable} t0, ${weighttableForThisFactor} t1
           ${weightJoinCondition};""", groundingDir)

        execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, description)
          SELECT id, isfixed, initvalue, ${weightDesc} FROM ${weighttableForThisFactor}
          ${incCondition};""")

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
          val weighttableForFactorOrderBy = if (dataStore.isUsingPostgresXL) "" else
             s"ORDER BY cardinality"

          dataStore.dropAndCreateTableAs(weighttableForThisFactor, s"""
            SELECT ${dataStore.cast(isFixed, "int")} AS isfixed, ${initvalue} AS initvalue,
            ${cardinalityCmd} AS cardinality, ${cweightid} AS id
            FROM ${cardinalityTables.mkString(", ")}
            ${weighttableForFactorOrderBy}""")

          // handle weight id
          val count = handleWeightIds("ORDER BY cardinality", true)
          updateIncWeightMeta()

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description)
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${weighttableForThisFactor}
            ${incCondition};""")

          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
            s"SELECT id AS factor_id, ${cweightid} AS weight_id, ${idcols} FROM ${querytable} ORDER BY id",
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

          // We need to order by weights and cardinality, in order for the sampler to
          // use an array-like access to the weights for multinomial factor.
          // For example, on http://deepdive.stanford.edu/doc/basics/schema.html,
          // Multinomial(a,b) is expanded to 6 indicator factors, each has a corresponding
          // weights. If the cardinalities are ordered, we can access the weights by using
          // the start address + the offset. See
          // https://github.com/HazyResearch/sampler/blob/master/src/dstruct/factor_graph/factor_graph.cpp#L51

          // We need to create two tables -- one for a non-order'ed version
          // another for an ordered version. The reason that we cannot
          // do this with only one table is not fundemental -- it is just
          // a specific property of Greenplum to make it right
          dataStore.dropAndCreateTableAs(s"${weighttableForThisFactor}_unsorted",
            s"""SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality,
            ${dataStore.cast(-1, "bigint")} AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0;""")

          // Similar to Greenplum, we need special handling for XL. The notion
          // of a physically sorted table that is sharded doesn't make sense, so
          // we keep XL's tables unsorted, but provide an order when we do sequence
          // assignment.
          val weighttableForFactorOrderBy = if (dataStore.isUsingPostgresXL) "" else
             s"ORDER BY ${weightlist}, cardinality"

          execute(s"""
            INSERT INTO ${weighttableForThisFactor}_unsorted
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} as cardinality, -1 AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")}
            ${weighttableForFactorOrderBy}""")

          dataStore.dropAndCreateTableAs(weighttableForThisFactor,
            s"""SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality,
            ${dataStore.cast(-1, "bigint")} AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0""")

          execute(s"""
            INSERT INTO ${weighttableForThisFactor}
            SELECT * FROM ${weighttableForThisFactor}_unsorted
            ${weighttableForFactorOrderBy}""")

          // handle weight id
          cweightid += handleWeightIds(s"ORDER BY ${weightlist}, cardinality", true)
          updateIncWeightMeta()

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description)
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${weighttableForThisFactor}
            ${incCondition};""")

          // use weight id corresponding to cardinality 0 (like C array...)
          val cardinalityKey = factorDesc.func.variables.map(v => "00000").mkString(",")

          // dump factors
          // TODO we don't have enough code reuse here.
          execute(dataStore.analyzeTable(querytable))
          execute(dataStore.analyzeTable(weighttableForThisFactor))

          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
            s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
             FROM ${querytable} t0, ${weighttableForThisFactor} t1
             WHERE ${weightJoinlist} AND t1.cardinality = '${cardinalityKey}' ORDER BY t0.id;""",
             groundingDir)
        }
      }
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

  // handle reusing last weights
  def reuseWeights(weightTable: String) {
    // skip learning: choose a table to copy weights from
    val fromWeightTable = weightTable.isEmpty() match {
      case true => lastWeightsTable
      case false => weightTable
    }
    log.info(s"""Using weights in TABLE ${fromWeightTable} by matching description""")

    // Already set -l 0 for sampler
    execute(s"""
      UPDATE ${WeightsTable} SET initvalue = weight
      FROM ${fromWeightTable}
      WHERE ${WeightsTable}.description = ${fromWeightTable}.description;
      """)
  }


  // create global meta data for incremental
  def createIncrementalMetaData() {
    dataStore.dropAndCreateTable(InferenceNamespace.getIncrementalMetaTableName(),
      s"id int, num_variables bigint, num_factors bigint, num_weights bigint")
    issueQuery(s"SELECT COUNT(*) FROM ${InferenceNamespace.getIncrementalMetaTableName()}") {
      rs => if (rs.getLong(1) == 0) {
        execute(s""" INSERT INTO ${InferenceNamespace.getIncrementalMetaTableName()} VALUES (0, 0, 0, 0); """)
      }
    }
  }


  /** Ground the factor graph to file
   *
   * Using the schema and inference rules defined in application.conf, construct factor
   * graph files.
   * Input: variable schema, factor descriptions, holdout configuration, database settings
   * Output: factor graph files: variables, factors, edges, weights, meta
   *
   * NOTE: for this to work in greenplum, do not put id as the first column!
   * The first column in greenplum is distribution key by default.
   * We need to update this column, but update is not allowed on distribution key.
   *
   * It is important to remember that we should not modify the user schema,
   * e.g., by adding columns to user relations. The right way to do it is
   * usually another. For example, an option could be creating a view of the
   * user relation, to which we add the needed column.
   *
   * It is also important to think about corner cases. For example, we cannot
   * assume any relation actually contains rows, or the rows are in some
   * predefined special order, or anything like that so the code must take care of
   * these cases, and there *must* be tests for the corner cases.
   *
   * TODO: This method is way too long and needs to be split, also for testing
   * purposes
   */
  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    calibrationSettings: CalibrationSettings, skipLearning: Boolean, weightTable: String,
    dbSettings: DbSettings) {

    val du = new DataLoader
    val parallelGrounding = dbSettings.gpload
    val groundingDir = getFileNameFromPath(Context.outputDir)
    val groundingPath = parallelGrounding match {
      case false => Context.outputDir
      case true => new java.io.File(dbSettings.gppath + s"/${groundingDir}").getCanonicalPath()
    }
    new java.io.File(groundingPath).mkdirs()

    log.info(s"Datastore type = ${Helpers.getDbType(dbSettings)}")
    log.info(s"Parallel grounding = ${parallelGrounding}")
    log.debug(s"Grounding Path = ${groundingPath}")

    // create global meta data
    dbSettings.incrementalMode match {
      case IncrementalMode.MATERIALIZATION => createIncrementalMetaData()
      case _ =>
    }

    val schemaForGrounding = dbSettings.incrementalMode match {
      case IncrementalMode.INCREMENTAL => schema.filter(_._1.startsWith("dd_delta_"))
      case _ => schema
    }

    // assign variable id - sequential and unique
    assignVariablesIds(schemaForGrounding, dbSettings)

    // assign holdout
    assignHoldout(schemaForGrounding, calibrationSettings)

    // generate cardinality tables
    generateCardinalityTables(schema)

    // generate factor meta data
    groundFactorMeta(du, factorDescs, dbSettings, groundingPath)

    // ground weights and factors
    groundFactorsAndWeights(factorDescs, calibrationSettings, du, dbSettings,
      groundingPath, skipLearning, weightTable)

    // ground variables
    groundVariables(schemaForGrounding, du, dbSettings, groundingPath)

    // create inference result table
    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    // split grounding files and transform to binary format
    convertGroundingFormat(groundingPath)

    // generate active compoenents for incremental
    dbSettings.incrementalMode match {
      case IncrementalMode.MATERIALIZATION => generateAcitve(groundingPath)
      case _ =>
    }
  }

  /**
   * weightsFile: binary format. Assume "weightsFile" file exists.
   */
  def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit
  /**
   * variablesFile: binary format. Assume "variablesFile" file exists.
   */
  def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit

  /**
   * This function is executed when sampler finished.
   */
  def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
    variableOutputFile: String, weightsOutputFile: String, dbSettings: DbSettings) = {

    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    log.info("Copying inference result weights...")
    bulkCopyWeights(weightsOutputFile, dbSettings)
    log.info("Copying inference result variables...")
    bulkCopyVariables(variableOutputFile, dbSettings)

    // Each (relation, column) tuple is a variable in the plate model.
     // Find all (relation, column) combinations
    val relationsColumns = variableSchema.keys map (_.split('.')) map {
      case Array(relation, column) => (relation, column)
    }

    // Create the view for mapped weights
    execute(createMappedWeightsViewSQL)

    relationsColumns.foreach { case(relationName, columnName) =>
      execute(createInferenceViewSQL(relationName, columnName))
    }
  }

  def getCalibrationData(variable: String, dataType: VariableDataType,
    buckets: List[Bucket]) : Map[Bucket, BucketData] = {

    val Array(relationName, columnName) = variable.split('.')
    val inferenceViewName = s"${relationName}_${columnName}_inference"
    val bucketedViewName = s"${relationName}_${columnName}_inference_bucketed"
    val calibrationViewName = s"${relationName}_${columnName}_calibration"

    execute(createBucketedCalibrationViewSQL(bucketedViewName, inferenceViewName, buckets))
    log.info(s"created calibration_view=${calibrationViewName}")
    dataType match {
      case BooleanType =>
        execute(createCalibrationViewBooleanSQL(calibrationViewName, bucketedViewName, columnName))
      case MultinomialType(_) =>
        execute(createCalibrationViewMultinomialSQL(calibrationViewName, bucketedViewName, columnName))
    }

    val bucketData = dataStore.selectAsMap(selectCalibrationDataSQL(calibrationViewName)).map { row =>
      val bucket = row("bucket")
      val data = BucketData(
        row.get("num_variables").map(_.asInstanceOf[Long]).getOrElse(0),
        row.get("num_correct").map(_.asInstanceOf[Long]).getOrElse(0),
        row.get("num_incorrect").map(_.asInstanceOf[Long]).getOrElse(0))
      (bucket, data)
    }.toMap
    buckets.zipWithIndex.map { case (bucket, index) =>
      (bucket, bucketData.get(index).getOrElse(BucketData(0,0,0)))
    }.toMap
  }
}
