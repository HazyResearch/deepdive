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
// import scala.collection.mutable.Map


/* Stores the factor graph and inference results. */
trait SQLInferenceDataStoreComponent extends InferenceDataStoreComponent {

  def inferenceDataStore : SQLInferenceDataStore

}


trait SQLInferenceDataStore extends InferenceDataStore with Logging {

  def ds : JdbcDataStore
  def dbSettings : DbSettings
  // def inferenceTableNameSpace : InferenceNamespace

  val factorOffset = new java.util.concurrent.atomic.AtomicLong(0)

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
  def FeatureStatsSupportTable = "dd_feature_statistics_support"
  def FeatureStatsView = "dd_feature_statistics"

    
  // Datastore-specific methods:
  // Below are methods to implement in any type of datastore.
    
  /**
   * Drop and create a sequence, based on database type.
   * 
   * @see http://dev.mysql.com/doc/refman/5.0/en/user-variables.html
   * @see http://www.it-iss.com/mysql/mysql-renumber-field-values/
   */
  def createSequenceFunction(seqName: String) : String
  
  /**
   * Get the next value of a sequence
   */
  def nextVal(seqName: String): String
  
  /**
   * Cast an expression to a type
   */
  def cast(expr: Any, toType: String): String
  
  /**
   * Given a string column name, Get a quoted version dependent on DB.
   *          if psql, return "column" 
   *          if mysql, return `column`
   */
  def quoteColumn(column: String) : String
  
  /**
   * Generate a random real number from 0 to 1.
   */
  def randomFunction : String
  
  /**
   * Concatenate a list of strings in the database.
   * @param list
   *     the list to concat
   * @param delimiter
   *     the delimiter used to seperate elements
   * @return
   *   Use '||' in psql, use 'concat' function in mysql
   */
  def concat(list: Seq[String], delimiter: String) : String

  // fast sequential id assign function
  def createAssignIdFunctionGreenplum() : Unit
  
  /**
   * ANALYZE TABLE
   */
  def analyzeTable(table: String) : String

  // assign senquential ids to table's id column
  def assignIds(table: String, startId: Long, sequence: String) : Long

  // end: Datastore-specific methods

  /** 
   * execute one or multiple SQL queries
   */
  def execute(sql: String) = {
    ds.executeSqlQueries(sql)
  }

  // execute a query (can have return results)
  def executeQuery(sql: String) = {
    ds.executeSqlQuery(sql)
  }

  /**
   * Issues a single SQL query that can return results, and perform {@code op} 
   * as callback function
   */
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {
    ds.executeSqlQueryWithCallback(sql)(op)
  }

  // used in unit test
  def keyType = "bigserial"
  def stringType = "text"
  def randomFunc = "RANDOM()"

  def checkGreenplumSQL = s"""
    SELECT version() LIKE '%Greenplum%';
  """

  def copyLastWeightsSQL = s"""
    DROP TABLE IF EXISTS ${lastWeightsTable} CASCADE;
    CREATE TABLE ${lastWeightsTable} AS
      SELECT X.*, Y.weight
      FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id;
  """

  def createInferenceResultSQL = s"""
    DROP TABLE IF EXISTS ${VariableResultTable} CASCADE; 
    CREATE TABLE ${VariableResultTable}(
      id bigint, 
      category bigint, 
      expectation double precision);
  """

  def createInferenceResultWeightsSQL = s"""
    DROP TABLE IF EXISTS ${WeightResultTable} CASCADE; 
    CREATE TABLE ${WeightResultTable}(
      id bigint primary key, 
      weight double precision);
  """

  def createInferenceResultIndicesSQL = s"""
    DROP INDEX IF EXISTS ${WeightResultTable}_idx CASCADE;
    DROP INDEX IF EXISTS ${VariableResultTable}_idx CASCADE;
    CREATE INDEX ${WeightResultTable}_idx ON ${WeightResultTable} (weight);
    CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} (expectation);
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
   * Create a table of how LR features are supported by supervision examples
   */
  def createFeatureStatsSupportTableSQL = 
      s"""DROP TABLE IF EXISTS ${FeatureStatsSupportTable} CASCADE;

          CREATE TABLE ${FeatureStatsSupportTable}(
            description text, 
            pos_examples bigint, 
            neg_examples bigint, 
            queries bigint);"""
  /**
   * Create a view that shows weights of features as well as their supports 
   */
  def createMappedFeatureStatsViewSQL = s"""
        CREATE OR REPLACE VIEW ${FeatureStatsView} AS
        SELECT w.*, f.pos_examples, f.neg_examples, f.queries
        FROM ${LearnedWeightsTable} w LEFT OUTER JOIN ${FeatureStatsSupportTable} f
        ON w.description = f.description
        ORDER BY abs(weight) DESC;
        """

  // ========= Datastore specific queries (override when diverge) ============

  /**
   * This query optimizes slow joins on certain DBMS (MySQL) by creating indexes
   * on the join condition column.
   */
  def createIndexForJoinOptimization(relation: String, column: String) = {
    // Default: No-op
  }

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
        executeQuery(s"CREATE INDEX ${queryTable}_${v}_idx ON ${queryTable}(${v}(255))") 
      } else {
        executeQuery(s"CREATE INDEX ${queryTable}_${v}_idx ON ${queryTable}(${v})")
      }
    })
  }
  
  def incrementId(table: String, IdSequence: String) {
    execute(s"UPDATE ${table} SET id = ${nextVal(IdSequence)};")
  }

  // assign variable id - sequential and unique
  def assignVariablesIds(schema: Map[String, _ <: VariableDataType]) {
    // fast sequential id assign function
    createAssignIdFunctionGreenplum()
    execute(createSequenceFunction(IdSequence))

    var idoffset : Long = 0
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      idoffset += assignIds(relation, idoffset, IdSequence)
    }
  }
  
  // assign variable holdout
  def assignHoldout(schema: Map[String, _ <: VariableDataType], calibrationSettings: CalibrationSettings) {
    // variable holdout table - if user defined, execute once
    ds.dropAndCreateTable(VariablesHoldoutTable, "variable_id bigint primary key")
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
            WHERE ${randomFunction} < ${calibrationSettings.holdoutFraction} AND ${column} IS NOT NULL;
            """)
        }
      }
    }

    // variable observation table
    ds.dropAndCreateTable(VariablesObservationTable, "variable_id bigint primary key")
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
      ds.dropAndCreateTable(cardinalityTableName, "cardinality text")
      execute(s"""
        INSERT INTO ${cardinalityTableName} VALUES ${cardinalityValues};
        """)
    }
  }
  
  // ground variables
  def groundVariables(schema: Map[String, _ <: VariableDataType], du: DataLoader, 
      dbSettings: DbSettings, parallelGrounding: Boolean, groundingPath: String) {
        schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      
      val variableDataType = InferenceNamespace.getVariableDataTypeId(dataType)

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
      val variableTypeTable = s"${relation}_vtype"
      execute(s"""
        DROP TABLE IF EXISTS ${variableTypeTable} CASCADE;
        CREATE TABLE ${variableTypeTable} AS
        SELECT t0.id, CASE WHEN t2.variable_id IS NOT NULL AND ${column} IS NOT NULL THEN 2
                           WHEN t1.variable_id IS NOT NULL THEN 0
                           WHEN ${column} IS NOT NULL THEN 1
                           ELSE 0
                      END as ${variableTypeColumn}
        FROM ${relation} t0 LEFT OUTER JOIN ${VariablesHoldoutTable} t1 
        ON t0.id=t1.variable_id LEFT OUTER JOIN ${VariablesObservationTable} t2 ON t0.id=t2.variable_id
      """)

      // Create an index on the id column of type table to optimize MySQL join, since MySQL uses BNLJ.
      // It's important to tailor join queries for MySQL as they don't have efficient join algorithms.
      // Specifically, we should create indexes on join condition columns (at least in MySQL implementation).
      createIndexForJoinOptimization(variableTypeTable, "id")

      // dump variables
      val initvalueCast = cast(cast(column, "int"), "float")
      // Sen
      // du.unload(s"dd_variables_${relation}", s"${groundingPath}/dd_variables_${relation}",
      val groundingDir = getFileNameFromPath(groundingPath)
      du.unload(InferenceNamespace.getVariableFileName(relation),
        s"${groundingPath}/${InferenceNamespace.getVariableFileName(relation)}",
        dbSettings, parallelGrounding,
        s"""SELECT t0.id, t1.${variableTypeColumn},
        CASE WHEN t1.${variableTypeColumn} = 0 THEN 0 ELSE ${initvalueCast} END AS initvalue,
        ${variableDataType} AS type, ${cardinality} AS cardinality
        FROM ${relation} t0, ${relation}_vtype t1
        WHERE t0.id=t1.id
        """, groundingDir)
    }
  }

  // ground factor meta data
  def groundFactorMeta(du: DataLoader, factorDescs: Seq[FactorDesc], groundingPath: String, 
    parallelGrounding: Boolean) {
    ds.dropAndCreateTable(FactorMetaTable, "name text, funcid int, sign text")

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
      dbSettings, parallelGrounding, s"SELECT * FROM ${FactorMetaTable}",
      groundingDir)
  }

  // create feature stats for boolean LR function
  def createFeatureStats(factorDesc: FactorDesc, querytable: String, weightlist: String,
    weightDesc: String) {
    // Create feature statistics support tables for error analysis, 
    // only if it's boolean LR feature (the most common one)
    if (factorDesc.func.variables.length == 1 && factorDesc.func.variableDataType == "Boolean") {
      // This should be a single variable, e.g. "is_true"
      val variableName = factorDesc.func.variables.map(v => 
          s""" ${quoteColumn(v.toString)} """).mkString(",")
      val groupByClause = weightlist match {
        case "" => ""
        case _ => s"GROUP BY ${weightlist}"
      }
      execute(s"""
      INSERT INTO ${FeatureStatsSupportTable}
      SELECT ${weightDesc} as description,
             count(CASE WHEN ${variableName}=TRUE THEN 1 ELSE NULL END) AS pos_examples,
             count(CASE WHEN ${variableName}=FALSE THEN 1 ELSE NULL END) AS neg_examples,
             count(CASE WHEN ${variableName} IS NULL THEN 1 ELSE NULL END) AS queries
      FROM ${querytable}
      ${groupByClause};
      """)
      execute(analyzeTable(FeatureStatsSupportTable))
    }
  }

  // convert grounding file format to be compatible with sampler
  // for more information about format, please refer to deepdive's website
  def convertGroundingFormat(groundingPath: String) {
    log.info("Converting grounding file format...")
    val ossuffix = if (System.getProperty("os.name").startsWith("Linux")) "linux" else "mac"
    // TODO: this python script is dangerous and ugly. It changes too many states!
    val cmd = s"python ${Context.deepdiveHome}/util/tobinary.py ${groundingPath} " + 
        s"${Context.deepdiveHome}/util/format_converter_${ossuffix} ${Context.outputDir}"
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

  def groundFactorsAndWeights(factorDescs: Seq[FactorDesc],
    calibrationSettings: CalibrationSettings, du: DataLoader,
    dbSettings: DbSettings, groundingPath: String, parallelGrounding: Boolean,
    skipLearning: Boolean, weightTable: String) {
    val groundingDir = getFileNameFromPath(groundingPath)

    // save last weights
    if (skipLearning && weightTable.isEmpty()) {
      execute(copyLastWeightsSQL)
    }

    // weights table
    ds.dropAndCreateTable(WeightsTable, """id bigint, isfixed int, initvalue real, cardinality text, 
      description text""")

    // weight and factor id
    // greenplum: use fast_seqassign postgres: use sequence
    var cweightid : Long = 0
    var factorid : Long = 0
    val weightidSequence = "dd_weight_sequence"
    val factoridSequence = "dd_factor_sequence"
    execute(createSequenceFunction(weightidSequence));
    execute(createSequenceFunction(factoridSequence));

    factorDescs.zipWithIndex.foreach { case (factorDesc, idx) =>
      // id columns
      val idcols = factorDesc.func.variables.map(v => 
        s""" ${quoteColumn(s"${v.relation}.id")} """).mkString(", ")
      // Sen
      // val querytable = s"dd_query_${factorDesc.name}"
      // val weighttableForThisFactor = s"dd_weights_${factorDesc.name}"
      val querytable = InferenceNamespace.getQueryTableName(factorDesc.name)
      val weighttableForThisFactor = InferenceNamespace.getWeightTableName(factorDesc.name)

      val outfile = InferenceNamespace.getFactorFileName(factorDesc.name)

      // table of input query
      execute(s"""DROP TABLE IF EXISTS ${querytable} CASCADE;
        CREATE TABLE ${querytable} AS ${factorDesc.inputQuery};""")
      execute(s"""ALTER TABLE ${querytable} ADD COLUMN id bigint;""")

      // handle factor id
      factorid += assignIds(querytable.toLowerCase(), factorid, factoridSequence)

      // weight variable list
      val weightlist = factorDesc.weight.variables.map(v => 
        s""" ${quoteColumn(v)} """).mkString(",")
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
        concat(weightVariables.map ( v => 
          s"""(CASE WHEN ${quoteColumn(v)} IS NULL THEN '' ELSE ${cast(quoteColumn(v), "text")} END)""" ), 
          "-") // Delimiter '-' for concat
          match {
            case "" => s"'${weightPrefix}-' "
            // concatinate the "prefix-" with the weight values
            case x => concat(Seq(s"'${weightPrefix}-' ", x), "")
      }
      val weightDesc = generateWeightDesc(factorDesc.weightPrefix, factorDesc.weight.variables)

      if (factorDesc.func.getClass.getSimpleName != "MultinomialFactorFunction") {

        // branch if weight variables present
        val hasWeightVariables = !(isFixed || weightlist == "")
        val createWeightTableForThisFactorSQL = hasWeightVariables match {
            // create a table that only contains one row (one weight) 
            case false => s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
              CREATE TABLE ${weighttableForThisFactor} AS
              SELECT ${cast(isFixed, "int")} AS isfixed, ${cast(initvalue, "float")} AS initvalue, 
                ${cast(0, "bigint")} AS id;"""
            // create one weight for each different element in weightlist.
            case true => s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
              CREATE TABLE ${weighttableForThisFactor} AS
              SELECT ${weightlist}, ${cast(isFixed, "int")} AS isfixed, ${cast(initvalue, "float")} AS initvalue, 
                ${cast(0, "bigint")} AS id
              FROM ${querytable}
              GROUP BY ${weightlist};"""
          }
          execute(createWeightTableForThisFactorSQL)

          // handle weight id
          cweightid += assignIds(weighttableForThisFactor.toLowerCase(), cweightid, weightidSequence)

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, description) 
            SELECT id, isfixed, initvalue, ${weightDesc} FROM ${weighttableForThisFactor};""")

          // check null weight (only if there are weight variables)
          if (hasWeightVariables) {
            val weightChecklist = factorDesc.weight.variables.map(v => s""" ${quoteColumn(v)} IS NULL """).mkString("AND")
            issueQuery(s"SELECT COUNT(*) FROM ${querytable} WHERE ${weightChecklist}") { rs =>
              if (rs.getLong(1) > 0) {
                throw new RuntimeException("Weight variable has null values")
              }
            }
          }

          // dump factors
          val weightjoinlist = factorDesc.weight.variables.map(
            v => s""" t0.${quoteColumn(v)} = t1.${quoteColumn(v)} """).mkString("AND")
          // do not have join conditions if there are no weight variables, and t1 will only have 1 row
          val weightJoinCondition = hasWeightVariables match {
            case true => "WHERE " + factorDesc.weight.variables.map(
                v => s""" t0.${quoteColumn(v)} = t1.${quoteColumn(v)} """).mkString("AND")
            case false => ""
          }
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
             FROM ${querytable} t0, ${weighttableForThisFactor} t1
             ${weightJoinCondition};""", groundingDir)

      } else if (factorDesc.func.getClass.getSimpleName == "MultinomialFactorFunction") {
        // TODO needs better code reuse
        // handle multinomial
        // generate cardinality table for each variable
        factorDesc.func.variables.zipWithIndex.foreach { case(v,idx) =>
          val cardinalityTableName = s"${factorDesc.weightPrefix}_cardinality_${idx}"
          execute(s"""
            DROP TABLE IF EXISTS ${cardinalityTableName} CASCADE;
            CREATE TABLE  ${cardinalityTableName} AS
            SELECT * FROM ${InferenceNamespace.getCardinalityTableName(v.headRelation, v.field)};
            """)
        }

        // cardinality values used in weight
        val cardinalityValues = factorDesc.func.variables.zipWithIndex.map { case(v,idx) => 
          s"""_c${idx}.cardinality"""
        }
        val cardinalityTables = factorDesc.func.variables.zipWithIndex.map { case(v,idx) =>
          s"${factorDesc.weightPrefix}_cardinality_${idx} AS _c${idx}"
        }
        val cardinalityCmd = s"""${concat(cardinalityValues,",")}"""

        // handle weights table
        // weight is fixed, or doesn't have weight variables
        if (isFixed || weightlist == ""){
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
            CREATE TABLE ${weighttableForThisFactor} AS
            SELECT ${cast(isFixed, "int")} AS isfixed, ${initvalue} AS initvalue, 
            ${cardinalityCmd} AS cardinality, ${cweightid} AS id
            FROM ${cardinalityTables.mkString(", ")}
            ORDER BY cardinality;""")

          // handle weight id
          val count = assignIds(weighttableForThisFactor.toLowerCase(), cweightid, weightidSequence)

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description) 
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${weighttableForThisFactor};""")

          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"SELECT id AS factor_id, ${cweightid} AS weight_id, ${idcols} FROM ${querytable}",
            groundingDir)

          // increment weight id
          cweightid += count

        } else { // not fixed and has weight variables
          // temporary weight table for weights without a cross product with cardinality
          val weighttableForThisFactorTemp = s"dd_weight_${factorDesc.name}_temp"

          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactorTemp} CASCADE;
                  CREATE TABLE ${weighttableForThisFactorTemp} AS
                  SELECT ${weightlist}, ${cast(isFixed, "int")} AS isfixed, ${cast(initvalue, "float")} AS initvalue
                  FROM ${querytable}
                  GROUP BY ${weightlist}; """)
  
          // We need to create two tables -- one for a non-order'ed version
          // another for an ordered version. The reason that we cannot
          // do this with only one table is not fundemental -- it is just
          // a specific property of Greenplum to make it right
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor}_unsorted CASCADE;
            CREATE TABLE ${weighttableForThisFactor}_unsorted AS 
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality, ${cast(0, "bigint")} AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0;""")

          execute(s"""
            INSERT INTO ${weighttableForThisFactor}_unsorted
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} as cardinality, 0 AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")}
            ORDER BY ${weightlist}, cardinality;""")
            
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
            CREATE TABLE ${weighttableForThisFactor} AS 
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality, ${cast(0, "bigint")} AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0;""")

          execute(s"""
            INSERT INTO ${weighttableForThisFactor}
            SELECT * FROM ${weighttableForThisFactor}_unsorted
            ORDER BY ${weightlist}, cardinality;""")

          // handle weight id
          cweightid += assignIds(weighttableForThisFactor.toLowerCase(), cweightid, weightidSequence)

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description) 
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${weighttableForThisFactor};""")

          // use weight id corresponding to cardinality 0 (like C array...)
          val cardinalityKey = factorDesc.func.variables.map(v => "00000").mkString(",")

          // dump factors
          // TODO we don't have enough code reuse here.
          val weightjoinlist = factorDesc.weight.variables.map(v => 
            s""" t0.${quoteColumn(v)} = t1.${quoteColumn(v)} """).mkString("AND")
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
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
      s"${groundingPath}/${InferenceNamespace.getWeightFileName}",dbSettings, parallelGrounding,
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
    dbSettings: DbSettings, parallelGrounding: Boolean) {

    val du = new DataLoader
    val groundingDir = getFileNameFromPath(Context.outputDir)
    val groundingPath = parallelGrounding match {
      case false => Context.outputDir 
      case true => dbSettings.gppath + s"/${groundingDir}"
    }
    new java.io.File(groundingPath).mkdirs()

    log.info(s"Datastore type = ${Helpers.getDbType(dbSettings)}")    
    log.info(s"Parallel grounding = ${parallelGrounding}")
    log.debug(s"Grounding Path = ${groundingPath}")

    // assign variable id - sequential and unique
    assignVariablesIds(schema)

    // assign holdout
    assignHoldout(schema, calibrationSettings)
    
    // generate cardinality tables
    generateCardinalityTables(schema)

    // ground variables
    groundVariables(schema, du, dbSettings, parallelGrounding, groundingPath)

    // generate factor meta data
    groundFactorMeta(du, factorDescs, groundingPath, parallelGrounding)
          
    // Create the feature stats table
    execute(createFeatureStatsSupportTableSQL)

    // ground weights and factors
    groundFactorsAndWeights(factorDescs, calibrationSettings, du, dbSettings, 
      groundingPath, parallelGrounding, skipLearning, weightTable)

    // create inference result table
    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    // split grounding files and transform to binary format
    convertGroundingFormat(groundingPath)
  }


  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String],
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String, 
    parallelGrounding: Boolean) : Unit = {

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
    //variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean) = {
    variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean, dbSettings: DbSettings) = {

    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    log.info("Copying inference result weights...")
    bulkCopyWeights(weightsOutputFile, dbSettings)
    log.info("Copying inference result variables...")
    bulkCopyVariables(variableOutputFile, dbSettings)
    log.info("Creating indices on the inference result...")
    execute(createInferenceResultIndicesSQL)

    // Each (relation, column) tuple is a variable in the plate model.
     // Find all (relation, column) combinations
    val relationsColumns = variableSchema.keys map (_.split('.')) map {
      case Array(relation, column) => (relation, column)
    }

    // Create the view for mapped weights
    execute(createMappedWeightsViewSQL)
    
    // Create feature statistics tables for error analysis
    execute(createMappedFeatureStatsViewSQL)

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
    
    val bucketData = ds.selectAsMap(selectCalibrationDataSQL(calibrationViewName)).map { row =>
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
