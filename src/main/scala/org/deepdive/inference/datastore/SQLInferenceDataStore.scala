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
  
  /**
   * ANALYZE TABLE
   */
  def analyzeTable(table: String) : String

  // end: Datastore-specific methods
  
  // TODO change integers to global constants / enums
  private def getFactorFunctionTypeid(functionName: String) = {
    functionName match {
      case "ImplyFactorFunction" => 0
      case "OrFactorFunction" => 1
      case "AndFactorFunction" => 2
      case "EqualFactorFunction" => 3
      case "IsTrueFactorFunction" =>  4
      case "MultinomialFactorFunction" => 5
    }
  }

  private def unwrapSQLType(x: Any) : Any = {
    x match {
      case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray().asInstanceOf[Array[_]].toList
      case x : org.postgresql.util.PGobject =>
        x.getType match {
          case "json" => Json.parse(x.getValue)
          case _ => JsNull
        }
      case x => x
    }
  }

  /** 
   * execute one or multiple SQL queries
   */
  def execute(sql: String) = {
    ds.executeSqlQueries(sql)
  }

  // execute a query (can have return results)
  private def executeQuery(sql: String) = {
    ds.executeSqlQuery(sql)
  }

  /**
   * Issues a single SQL query that can return results, and perform {@code op} 
   * as callback function
   */
  private def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {
    ds.executeSqlQueryWithCallback(sql)(op)
  }

  // execute sql, store results in a map
  def selectAsMap(sql: String) : List[Map[String, Any]] = {
    val conn = ds.borrowConnection()
    conn.setAutoCommit(false)
    try {
      val stmt = conn.createStatement(
        java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY)
      stmt.setFetchSize(10000)
      val rs = stmt.executeQuery(sql)
      // No result return
      if (!rs.isBeforeFirst) {
        log.warning(s"query returned no results: ${sql}")
        Iterator.empty.toSeq
      } else {
        val resultIter = new Iterator[Map[String, Any]] {
          def hasNext = {
            // TODO: This is expensive
            !(rs.isLast)
          }              
          def next() = {
            rs.next()
            val metadata = rs.getMetaData()
            (1 to metadata.getColumnCount()).map { i => 
              val label = metadata.getColumnLabel(i)
              val data = unwrapSQLType(rs.getObject(i))
              (label, data)
            }.filter(_._2 != null).toMap
          }
        }
        resultIter.toSeq
      }
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
        log.error(exception.toString)
        throw exception
    } finally {
      conn.close()
    }


    ds.DB.readOnly { implicit session =>
      SQL(sql).map(_.toMap).list.apply()
    }
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
      FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id
      ORDER BY id ASC;
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
  // TODO
  def WRONGcreateCalibrationViewRealNumberSQL(name: String, bucketedView: String, columnName: String) = s"""
      CREATE OR REPLACE VIEW ${name} AS
      SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
      (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
      LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView} 
        WHERE ${columnName}=0.0 GROUP BY bucket) b2 ON b1.bucket = b2.bucket
      LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView} 
        WHERE ${columnName}=0.0 GROUP BY bucket) b3 ON b1.bucket = b3.bucket 
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

  /** 
   *  TODO: Now this is specific for greenplum and never used elsewhere. needs refactoring. 
   */
  def createAssignIdFunctionGreenplum = 
    """
    DROP LANGUAGE IF EXISTS plpgsql CASCADE;
    DROP LANGUAGE IF EXISTS plpythonu CASCADE;
    CREATE LANGUAGE plpgsql;
    CREATE LANGUAGE plpythonu;

    CREATE OR REPLACE FUNCTION clear_count_1(sid int) RETURNS int AS 
    $$
    if '__count_1' in SD:
      SD['__count_1'] = -1
      return 1
    return 0
    $$ LANGUAGE plpythonu;
     
     
    CREATE OR REPLACE FUNCTION updateid(startid bigint, sid int, sids int[], base_ids bigint[], base_ids_noagg bigint[]) RETURNS bigint AS 
    $$
    if '__count_1' in SD:
      a = SD['__count_2']
      b = SD['__count_1']
      SD['__count_2'] = SD['__count_2'] - 1
      if SD['__count_2'] < 0:
        SD.pop('__count_1')
      return startid+b-a
    else:
      for i in range(0, len(sids)):
        if sids[i] == sid:
          SD['__count_1'] = base_ids[i] - 1
          SD['__count_2'] = base_ids_noagg[i] - 1
      a = SD['__count_2']
      b = SD['__count_1']
      SD['__count_2'] = SD['__count_2'] - 1
      if SD['__count_2'] < 0:
        SD.pop('__count_1')
      return startid+b-a
      
    $$ LANGUAGE plpythonu;
     
    CREATE OR REPLACE FUNCTION fast_seqassign(tname character varying, startid bigint) RETURNS TEXT AS 
    $$
    BEGIN
      EXECUTE 'drop table if exists tmp_gpsid_count cascade;';
      EXECUTE 'drop table if exists tmp_gpsid_count_noagg cascade;';
      EXECUTE 'create table tmp_gpsid_count as select gp_segment_id as sid, count(clear_count_1(gp_segment_id)) as base_id from ' || quote_ident(tname) || ' group by gp_segment_id order by sid distributed by (sid);';
      EXECUTE 'create table tmp_gpsid_count_noagg as select * from tmp_gpsid_count distributed by (sid);';
      EXECUTE 'update tmp_gpsid_count as t set base_id = (SELECT SUM(base_id) FROM tmp_gpsid_count as t2 WHERE t2.sid <= t.sid);';
      RAISE NOTICE 'EXECUTING _fast_seqassign()...';
      EXECUTE 'select * from _fast_seqassign(''' || quote_ident(tname) || ''', ' || startid || ');';
      RETURN '';
    END;
    $$ LANGUAGE 'plpgsql';
     
    CREATE OR REPLACE FUNCTION _fast_seqassign(tname character varying, startid bigint)
    RETURNS TEXT AS
    $$
    DECLARE
      sids int[] :=  ARRAY(SELECT sid FROM tmp_gpsid_count ORDER BY sid);
      base_ids bigint[] :=  ARRAY(SELECT base_id FROM tmp_gpsid_count ORDER BY sid);
      base_ids_noagg bigint[] :=  ARRAY(SELECT base_id FROM tmp_gpsid_count_noagg ORDER BY sid);
      tsids text;
      tbase_ids text;
      tbase_ids_noagg text;
    BEGIN
      SELECT INTO tsids array_to_string(sids, ',');
      SELECT INTO tbase_ids array_to_string(base_ids, ',');
      SELECT INTO tbase_ids_noagg array_to_string(base_ids_noagg, ',');
      if ('update ' || tname || ' set id = updateid(' || startid || ', gp_segment_id, ARRAY[' || tsids || '], ARRAY[' || tbase_ids || '], ARRAY[' || tbase_ids_noagg || ']);')::text is not null then
        EXECUTE 'update ' || tname || ' set id = updateid(' || startid || ', gp_segment_id, ARRAY[' || tsids || '], ARRAY[' || tbase_ids || '], ARRAY[' || tbase_ids_noagg || ']);';
      end if;
      RETURN '';
    END;
    $$
    LANGUAGE 'plpgsql';
    """

  def init() : Unit = {
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
        
  // clean up grounding folder (for parallel grounding)
  // TODO: we may not need to actually create a script for this and execute it.
  // We may want to directly execute the commands:
  // Helpers.executeCmd(s"rm -rf ${groundingPath}/dd_tmp")
  // Helpers.executeCmd(s"rm -f ${groundingPath}/dd_*")
  // XXX: This function is a little risky because groundingPath may be the empty
  // string. Shall we avoid letting the user shoot her own foot?
  def cleanParallelGroundingPath(groundingPath: String) {
    val cleanFile = File.createTempFile(s"clean", ".sh")
    val writer = new PrintWriter(cleanFile)
    // cleaning up remaining tmp folder for tobinary
    writer.println(s"rm -rf ${groundingPath}/dd_tmp")
    writer.println(s"rm -f ${groundingPath}/dd_*")
    writer.close()
    log.info("Cleaning up grounding folder...")
    Helpers.executeCmd(cleanFile.getAbsolutePath())
  }

  // assign variable id - sequential and unique
  def assignVariablesIds(schema: Map[String, _ <: VariableDataType]) {
    // check whether Greenplum is used
    var usingGreenplum = false
    issueQuery(checkGreenplumSQL) { rs => 
      usingGreenplum = rs.getBoolean(1) 
    }

    var idoffset : Long = 0
    if (usingGreenplum) {
      // We use a special homemade function for Greenplum
      executeQuery(createAssignIdFunctionGreenplum)
      schema.foreach { case(variable, dataType) =>
        val Array(relation, column) = variable.split('.')
        executeQuery(s"""SELECT fast_seqassign('${relation.toLowerCase()}', ${idoffset});""")
        issueQuery(s"""SELECT count(*) FROM ${relation}""") { rs =>
          idoffset = idoffset + rs.getLong(1)
        }
      }
    } else {
      // Mysql: use user-defined variables for ID assign;
      // Psql: use sequence
      execute(createSequenceFunction(IdSequence))
      schema.foreach { case(variable, dataType) =>
        val Array(relation, column) = variable.split('.')
        incrementId(relation, IdSequence)
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
    dbSettings: DbSettings, parallelGrounding: Boolean) {

    val du = new DataLoader
    val groundingPath = if (!parallelGrounding) Context.outputDir else dbSettings.gppath

    // check whether Greenplum is used
    var usingGreenplum = false
    issueQuery(checkGreenplumSQL) { rs => 
      usingGreenplum = rs.getBoolean(1) 
    }
    
    log.info(s"Using Greenplum = ${usingGreenplum}")
    log.info(s"Datastore type = ${Helpers.getDbType(dbSettings)}")
    
    log.info(s"Parallel grounding = ${parallelGrounding}")
    log.debug(s"Grounding Path = ${groundingPath}")

    // clean up grounding folder (for parallel grounding)
    if (parallelGrounding) {
      cleanParallelGroundingPath(groundingPath)
    }

    // assign variable id - sequential and unique
    assignVariablesIds(schema)

    // variable holdout table - if user defined, execute once
    execute(s"""DROP TABLE IF EXISTS ${VariablesHoldoutTable} CASCADE;
      CREATE TABLE ${VariablesHoldoutTable}(variable_id bigint primary key);
      """)
    calibrationSettings.holdoutQuery match {
      case Some(query) => execute(query)
      case None =>
    }

    // variable observation table
    execute(s"""DROP TABLE IF EXISTS ${VariablesObservationTable} CASCADE;
      CREATE TABLE ${VariablesObservationTable}(variable_id bigint primary key);
      """)
    calibrationSettings.observationQuery match {
      case Some(query) => execute(query)
      case None =>
    }

    // ground variables
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      val variableDataType = dataType match {
        case BooleanType => 0
        case MultinomialType(x) => 1
        case RealNumberType => 2
        case RealArrayType(x) => 3
      }

      val cardinality = dataType match {
        case BooleanType => 2
        case MultinomialType(x) => x.toInt
        case RealNumberType => 2
        case RealArrayType(x) => x.toInt
      }

      if(variableDataType == 2 || variableDataType == 3){

      } else {
        // This cannot be parsed in def randFunc for now.
        // assign holdout - if not user-defined, randomly select from evidence variables of each variable table
        calibrationSettings.holdoutQuery match {
          case Some(s) =>
          case None => execute(s"""
            INSERT INTO ${VariablesHoldoutTable}
            SELECT id FROM ${relation}
            WHERE ${randomFunction} < ${calibrationSettings.holdoutFraction} AND ${column} IS NOT NULL;
            """)
        }
      }


      // Create a cardinality table for the variable
      // note we use five-digit fixed-length representation here
      val cardinalityValues = dataType match {
        case BooleanType => "('00001')"
        case MultinomialType(x) => (0 to x-1).map (n => s"""('${"%05d".format(n)}')""").mkString(", ")
        case RealNumberType => "('00001')"
        case RealArrayType(x) => "('00001')"
      }
      val cardinalityTableName = s"${relation}_${column}_cardinality"
      execute(s"""
        DROP TABLE IF EXISTS ${cardinalityTableName} CASCADE;
        CREATE TABLE ${cardinalityTableName}(cardinality text);
        INSERT INTO ${cardinalityTableName} VALUES ${cardinalityValues};
        """)

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
      val initvalueCast = variableDataType match {
        case 2 | 3 => cast(column, "float")
        case _ => cast(cast(column, "int"), "float")
      }
      du.unload(s"dd_variables_${relation}", s"${groundingPath}/dd_variables_${relation}",
        dbSettings, parallelGrounding,
        s"""SELECT t0.id, t1.${variableTypeColumn},
        CASE WHEN t1.${variableTypeColumn} = 0 THEN 0 ELSE ${initvalueCast} END AS initvalue,
        ${variableDataType} AS type, ${cardinality} AS cardinality
        FROM ${relation} t0, ${relation}_vtype t1
        WHERE t0.id=t1.id
        """)

    }

    // generate factor meta data
    execute(s"""DROP TABLE IF EXISTS ${FactorMetaTable} CASCADE;
      CREATE TABLE ${FactorMetaTable} (name text, funcid int, sign text);
      """)

    // generate a string containing the signs (whether negated) of variables for each factor
    factorDescs.foreach { factorDesc =>
      val signString = factorDesc.func.variables.map(v => !v.isNegated).mkString(" ")
      val funcid = getFactorFunctionTypeid(factorDesc.func.getClass.getSimpleName)
      execute(s"INSERT INTO ${FactorMetaTable} VALUES ('${factorDesc.name}', ${funcid}, '${signString}')")
    }

    // dump factor meta data
    du.unload(s"dd_factormeta", s"${groundingPath}/dd_factormeta", dbSettings, parallelGrounding,
      s"SELECT * FROM ${FactorMetaTable}")

    // weights table
    execute(s"""DROP TABLE IF EXISTS ${WeightsTable} CASCADE;""")
    execute(s"""CREATE TABLE ${WeightsTable} (id bigint, isfixed int, initvalue real, 
      cardinality text, description text);""")

    // weight and factor id
    // greenplum: use fast_seqassign postgres: use sequence
    var cweightid : Long = 0
    var factorid : Long = 0
    val weightidSequence = "dd_weight_sequence"
    val factoridSequence = "dd_factor_sequence"
    if (!usingGreenplum) {
      execute(createSequenceFunction(weightidSequence));
      execute(createSequenceFunction(factoridSequence));
    }

          
    // Create the feature stats table
    execute(createFeatureStatsSupportTableSQL)

    // ground weights and factors
    factorDescs.zipWithIndex.foreach { case (factorDesc, idx) =>
      // id columns
      val idcols = factorDesc.func.variables.map(v => 
        s""" ${quoteColumn(s"${v.relation}.id")} """).mkString(", ")
      val querytable = s"dd_query_${factorDesc.name}"
      val weighttableForThisFactor = s"dd_weights_${factorDesc.name}"
      val outfile = s"dd_factors_${factorDesc.name}_out"

      // table of input query
      execute(s"""DROP TABLE IF EXISTS ${querytable} CASCADE;
        CREATE TABLE ${querytable} AS ${factorDesc.inputQuery};""")
      execute(s"""ALTER TABLE ${querytable} ADD COLUMN id bigint;""")

      // handle factor id
      if (usingGreenplum) {
        executeQuery(s"SELECT fast_seqassign('${querytable.toLowerCase()}', ${factorid});");
      } else {
        execute(s"UPDATE ${querytable} SET id = ${nextVal(factoridSequence)};")
      }
      issueQuery(s"""SELECT COUNT(*) FROM ${querytable};""") { rs =>
        factorid += rs.getLong(1)
      }

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
          if (usingGreenplum) {      
            executeQuery(s"""SELECT fast_seqassign('${weighttableForThisFactor.toLowerCase()}', ${cweightid});""")
          } else {
            execute(s"UPDATE ${weighttableForThisFactor} SET id = ${nextVal(weightidSequence)};")
          }
          issueQuery(s"""SELECT COUNT(*) FROM ${weighttableForThisFactor};""") { rs =>
            cweightid += rs.getLong(1)
          }

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
          val weightjoinlist = factorDesc.weight.variables.map(v => s""" t0.${quoteColumn(v)} = t1.${quoteColumn(v)} """).mkString("AND")
          // do not have join conditions if there are no weight variables, and t1 will only have 1 row
          val weightJoinCondition = hasWeightVariables match {
            case true => "WHERE " + factorDesc.weight.variables.map(
                v => s""" t0.${quoteColumn(v)} = t1.${quoteColumn(v)} """).mkString("AND")
            case false => ""
          }
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
             FROM ${querytable} t0, ${weighttableForThisFactor} t1
             ${weightJoinCondition};""")

      } else if (factorDesc.func.getClass.getSimpleName == "MultinomialFactorFunction") {
        // TODO needs better code reuse
        // handle multinomial
        // generate cardinality table for each variable
        factorDesc.func.variables.zipWithIndex.foreach { case(v,idx) =>
          val cardinalityTableName = s"${factorDesc.weightPrefix}_cardinality_${idx}"
          execute(s"""
            DROP TABLE IF EXISTS ${cardinalityTableName} CASCADE;
            CREATE TABLE  ${cardinalityTableName} AS
            SELECT * FROM ${v.headRelation}_${v.field}_cardinality;
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
            SELECT ${cast(isFixed, "int")} AS isfixed, ${initvalue} AS initvalue, ${cardinalityCmd} AS cardinality, ${cweightid} AS id
            FROM ${cardinalityTables.mkString(", ")}
            ORDER BY cardinality;""")

          // handle weight id
          if (usingGreenplum) {      
            executeQuery(s"""SELECT fast_seqassign('${weighttableForThisFactor.toLowerCase()}', ${cweightid});""")
          } else {
            execute(s"UPDATE ${weighttableForThisFactor} SET id = ${nextVal(weightidSequence)};")
          }

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description) 
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${weighttableForThisFactor};""")

          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"SELECT id AS factor_id, ${cweightid} AS weight_id, ${idcols} FROM ${querytable}")

          // increment weight id
          issueQuery(s"""SELECT COUNT(*) FROM ${weighttableForThisFactor};""") { rs =>
            cweightid += rs.getLong(1)
          }
        } else { // not fixed and has weight variables
          // temporary weight table for weights without a cross product with cardinality
          val weighttableForThisFactorTemp = s"dd_weight_${factorDesc.name}_temp"

          if (usingGreenplum) {
             execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactorTemp} CASCADE;
                  CREATE TABLE ${weighttableForThisFactorTemp} AS 
                    (SELECT ${weightlist}, ${cast(isFixed, "int")} AS isfixed, ${cast(initvalue, "float")} AS initvalue
                    FROM ${querytable}
                    GROUP BY ${weightlist}) DISTRIBUTED BY (${weightlist});""") 
            }else{
              execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactorTemp} CASCADE;
                      CREATE TABLE ${weighttableForThisFactorTemp} AS
                      SELECT ${weightlist}, ${cast(isFixed, "int")} AS isfixed, ${cast(initvalue, "float")} AS initvalue
                      FROM ${querytable}
                      GROUP BY ${weightlist}; """)
            }       
  
          // We need to create two tables -- one for a non-order'ed version
          // another for an ordered version. The reason that we cannot
          // do this with only one table is not fundemental -- it is just
          // a specific property of Greenplum to make it right
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
            CREATE TABLE ${weighttableForThisFactor} AS 
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0;""")

          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor}_unsorted CASCADE;
            CREATE TABLE ${weighttableForThisFactor}_unsorted AS 
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0;""")

          execute(s"""ALTER TABLE ${weighttableForThisFactor} ADD COLUMN id bigint;""")
          execute(s"""ALTER TABLE ${weighttableForThisFactor}_unsorted ADD COLUMN id bigint;""")

          execute(s"""
            INSERT INTO ${weighttableForThisFactor}_unsorted
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} as cardinality, 0 AS id
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")}
            ORDER BY ${weightlist}, cardinality;""")

          execute(s"""
            INSERT INTO ${weighttableForThisFactor}
            SELECT * FROM ${weighttableForThisFactor}_unsorted
            ORDER BY ${weightlist}, cardinality;""")

          // handle weight id
          if (usingGreenplum) {      
            executeQuery(s"""SELECT fast_seqassign('${weighttableForThisFactor.toLowerCase()}', ${cweightid});""")
          } else {
            execute(s"UPDATE ${weighttableForThisFactor} SET id = ${nextVal(weightidSequence)};")
          }
          issueQuery(s"""SELECT COUNT(*) FROM ${weighttableForThisFactor};""") { rs =>
            cweightid += rs.getLong(1)
          }

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
             WHERE ${weightjoinlist} AND t1.cardinality = '${cardinalityKey}';""")
        }
      }
      
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

    // dump weights
    du.unload("dd_weights", s"${groundingPath}/dd_weights",dbSettings, parallelGrounding,
      s"SELECT id, isfixed, COALESCE(initvalue, 0) FROM ${WeightsTable}")

    // create inference result table
    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    // split grounding files and transform to binary format
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
      case RealNumberType =>
        execute(WRONGcreateCalibrationViewRealNumberSQL(calibrationViewName, bucketedViewName, columnName))
      case RealArrayType(x) => 
        execute(WRONGcreateCalibrationViewRealNumberSQL(calibrationViewName, bucketedViewName, columnName))
    }
    
    val bucketData = selectAsMap(selectCalibrationDataSQL(calibrationViewName)).map { row =>
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
