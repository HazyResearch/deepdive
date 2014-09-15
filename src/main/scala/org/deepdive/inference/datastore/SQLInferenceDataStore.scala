package org.deepdive.inference

import java.io.{File, PrintWriter}
import org.deepdive.calibration._
import org.deepdive.datastore.JdbcDataStore
import org.deepdive.datastore.DataUnloader
import org.deepdive.Logging
import org.deepdive.Context
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

  def getFactorFunctionTypeid(functionName: String) = {
    functionName match {
      case "ImplyFactorFunction" => 0
      case "OrFactorFunction" => 1
      case "AndFactorFunction" => 2
      case "EqualFactorFunction" => 3
      case "IsTrueFactorFunction" =>  4
      case "MultinomialFactorFunction" => 5
    }
  }


  def unwrapSQLType(x: Any) : Any = {
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

  // execute a query
  def execute(sql: String) = {
    log.debug("EXECUTING.... " + sql)
    val conn = ds.borrowConnection()
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_UPDATABLE)
    try {
      """;\s+""".r.split(sql.trim()).filterNot(_.isEmpty).foreach(q => 
        conn.prepareStatement(q.trim()).executeUpdate)
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close()
    }
    log.debug("DONE!")
  }

  // execute a query (can have return results)
  def executeQuery(sql: String) = {
    log.debug("EXECUTING.... " + sql)
    val conn = ds.borrowConnection()
    conn.setAutoCommit(false)
    val stmt = conn.createStatement();
    stmt.execute(sql)
    conn.commit()
    conn.close()
    log.debug("DONE!")
  }

  // issues a query, and perform op
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {
    log.debug("EXECUTING... " + sql)
    val conn = ds.borrowConnection()
    try {
      conn.setAutoCommit(false);
      val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
        java.sql.ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(5000);
      val rs = stmt.executeQuery(sql)
      while(rs.next()){
        op(rs)
      }
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close() 
    }
    log.debug("DONE!")
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
    SELECT X.*, Y.weight INTO ${lastWeightsTable}
      FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id
      ORDER BY id ASC;
  """

  def createSequencesSQL = s"""
    DROP SEQUENCE IF EXISTS ${IdSequence} CASCADE;
    CREATE SEQUENCE ${IdSequence} MINVALUE -1 START 0;
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

  def createInferenceResultIndiciesSQL = s"""
    DROP INDEX IF EXISTS ${WeightResultTable}_idx CASCADE;
    DROP INDEX IF EXISTS ${VariableResultTable}_idx CASCADE;
    CREATE INDEX ${WeightResultTable}_idx ON ${WeightResultTable} (weight);
    CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} (expectation);
  """

  def createInferenceViewSQL(relationName: String, columnName: String) = s"""
    CREATE VIEW ${relationName}_${columnName}_inference AS
    (SELECT ${relationName}.*, mir.category, mir.expectation FROM
    ${relationName}, ${VariableResultTable} mir
    WHERE ${relationName}.id = mir.id
    ORDER BY mir.expectation DESC);
  """

  def createBucketedCalibrationViewSQL(name: String, inferenceViewName: String, buckets: List[Bucket]) = {
    val bucketCaseStatement = buckets.zipWithIndex.map { case(bucket, index) =>
      s"WHEN expectation >= ${bucket.from} AND expectation <= ${bucket.to} THEN ${index}"
    }.mkString("\n")
    s"""CREATE VIEW ${name} AS
      SELECT ${inferenceViewName}.*, CASE ${bucketCaseStatement} END bucket
      FROM ${inferenceViewName} ORDER BY bucket ASC;"""
  }

  def createCalibrationViewBooleanSQL(name: String, bucketedView: String, columnName: String) =  s"""
    CREATE VIEW ${name} AS
    SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
    (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
    LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView} 
      WHERE ${columnName}=true GROUP BY bucket) b2 ON b1.bucket = b2.bucket
    LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView} 
      WHERE ${columnName}=false GROUP BY bucket) b3 ON b1.bucket = b3.bucket 
    ORDER BY b1.bucket ASC;
  """

  def createCalibrationViewMultinomialSQL(name: String, bucketedView: String, columnName: String) =  s"""
    CREATE VIEW ${name} AS
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
    CREATE VIEW ${VariableResultTable}_mapped_weights AS
    SELECT ${WeightsTable}.*, ${WeightResultTable}.weight FROM
    ${WeightsTable} JOIN ${WeightResultTable} ON ${WeightsTable}.id = ${WeightResultTable}.id
    ORDER BY abs(weight) DESC;
  """

  def createAssignIdFunctionSQL = 
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
      EXECUTE 'update ' || tname || ' set id = updateid(' || startid || ', gp_segment_id, ARRAY[' || tsids || '], ARRAY[' || tbase_ids || '], ARRAY[' || tbase_ids_noagg || ']);';
      RETURN '';
    END;
    $$
    LANGUAGE 'plpgsql';
    """


  def executeCmd(cmd: String) : Try[Int] = {
    // Make the file executable, if necessary
    val file = new java.io.File(cmd)
    if (file.isFile) file.setExecutable(true, false)
    log.info(s"""Executing: "$cmd" """)
    val processLogger = ProcessLogger(line => log.info(line))
    Try(cmd!(processLogger)) match {
      case Success(0) => Success(0)
      case Success(errorExitValue) => 
        Failure(new RuntimeException(s"Script exited with exit_value=$errorExitValue"))
      case Failure(ex) => Failure(ex)
    }
  }


  def init() : Unit = {
  }

  /** Ground the factor graph to file
   *
   * Using the schema and inference rules defined in application.conf, construct factor
   * graph files.
   * Input: variable schema, factor descriptions, holdout configuration, database settings
   * Output: factor graph files: variables, factors, edges, weights, meta
   */
  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String, 
    dbSettings: DbSettings, parallelGrounding: Boolean) {

    val du = new DataUnloader
    val groundingPath = if (!parallelGrounding) Context.outputDir else dbSettings.gppath

    // check whether Greenplum is used
    var usingGreenplum = false
    issueQuery(checkGreenplumSQL) { rs => 
      usingGreenplum = rs.getBoolean(1) 
    }
    log.info(s"Using Greenplum = ${usingGreenplum}")
    log.info(s"Parallel grounding = ${parallelGrounding}")
    log.debug(s"Grounding Path = ${groundingPath}")

    // clean up grounding folder (for parallel grounding)
    if (parallelGrounding) {
      val cleanFile = File.createTempFile(s"clean", ".sh")
      val writer = new PrintWriter(cleanFile)
      writer.println(s"rm -f ${groundingPath}/dd_*")
      writer.close()
      log.info("Cleaning up grounding folder...")
      executeCmd(cleanFile.getAbsolutePath())
    }

    // assign variable id - sequential and unique
    // for greenplum, we use a special fast assign sequential id function
    var idoffset : Long = 0
    if(usingGreenplum) {
      executeQuery(createAssignIdFunctionSQL)
      schema.foreach { case(variable, dataType) =>
        val Array(relation, column) = variable.split('.')
        executeQuery(s"""SELECT fast_seqassign('${relation.toLowerCase()}', ${idoffset});""")
        issueQuery(s"""SELECT max(id) FROM ${relation}""") { rs =>
          idoffset = 1 + rs.getLong(1)
        }
      }
    } else {
      execute(s"""DROP SEQUENCE IF EXISTS ${IdSequence} CASCADE;
        CREATE SEQUENCE ${IdSequence} MINVALUE -1 START 0;""")
      schema.foreach { case(variable, dataType) =>
        val Array(relation, column) = variable.split('.')
        execute(s"UPDATE ${relation} SET id = nextval('${IdSequence}');")
      }
    }

    // variable holdout table - if user defined, execute once
    execute(s"""DROP TABLE IF EXISTS ${VariablesHoldoutTable} CASCADE;
      CREATE TABLE ${VariablesHoldoutTable}(variable_id bigint primary key);
      """)
    holdoutQuery match {
      case Some(query) => execute(query)
      case None =>
    }

    // ground variables
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      val variableDataType = dataType match {
        case BooleanType => 0
        case MultinomialType(x) => 1
      }

      val cardinality = dataType match {
        case BooleanType => 2
        case MultinomialType(x) => x.toInt
      }

      // assign holdout - if not user-defined, randomly select from evidence variables of each variable table
      holdoutQuery match {
        case Some(s) =>
        case None => execute(s"""
          INSERT INTO ${VariablesHoldoutTable}
          SELECT id FROM ${relation}
          WHERE RANDOM() < ${holdoutFraction} AND ${column} IS NOT NULL;
          """)
      }

      // Create a cardinality table for the variable
      // note we use two-digit fixed-length representation here (may be fixed)
      val cardinalityValues = dataType match {
        case BooleanType => "('00001')"
        case MultinomialType(x) => (0 to x-1).map (n => s"""('${"%05d".format(n)}')""").mkString(", ")
      }
      val cardinalityTableName = s"${relation}_${column}_cardinality"
      execute(s"""
        DROP TABLE IF EXISTS ${cardinalityTableName} CASCADE;
        CREATE TABLE ${cardinalityTableName}(cardinality text);
        INSERT INTO ${cardinalityTableName} VALUES ${cardinalityValues};
        """)

      // dump variables, 
      // variable table join with holdout table - a variable is an evidence if it has initial value and it is not holdout
      du.unload(s"dd_variables_${relation}", s"${groundingPath}/dd_variables_${relation}", dbSettings, parallelGrounding,
        s"""SELECT id, 1::int AS is_evidence, ${column}::int AS initvalue, ${variableDataType} AS type, 
          ${cardinality} AS cardinality  
        FROM ${relation} LEFT OUTER JOIN ${VariablesHoldoutTable}
        ON ${relation}.id = ${VariablesHoldoutTable}.variable_id
        WHERE ${column} IS NOT NULL AND ${VariablesHoldoutTable}.variable_id IS NULL
        UNION ALL
        SELECT id, 0::int AS is_evidence, 0::int AS initvalue, ${variableDataType} AS type, 
          ${cardinality} AS cardinality  
        FROM ${relation} LEFT OUTER JOIN ${VariablesHoldoutTable}
        ON ${relation}.id = ${VariablesHoldoutTable}.variable_id
        WHERE ${column} IS NULL OR ${VariablesHoldoutTable}.variable_id IS NOT NULL;
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
    execute(s"""CREATE TABLE ${WeightsTable} (id bigint, isfixed int, initvalue real, cardinality text);""")

    // weight and factor id
    // greemplum: use fast_seqassign postgres: use sequence
    var cweightid : Long = 0
    var factorid : Long = 0
    val weightidSequence = "dd_weight_sequence"
    val factoridSequence = "dd_factor_sequence"
    if (!usingGreenplum) {
      execute(s"""DROP SEQUENCE IF EXISTS ${weightidSequence} CASCADE;
        CREATE SEQUENCE ${weightidSequence} MINVALUE -1 START 0;""") 
      execute(s"""DROP SEQUENCE IF EXISTS ${factoridSequence} CASCADE;
        CREATE SEQUENCE ${factoridSequence} MINVALUE -1 START 0;""") 
    }

    // ground weights and factors
    factorDescs.zipWithIndex.foreach { case (factorDesc, idx) =>
      // id columns
      val idcols = factorDesc.func.variables.map(v => s""" "${v.relation}.id" """).mkString(", ")
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
        execute(s"UPDATE ${querytable} SET id = nextval('${factoridSequence}');")
      }
      issueQuery(s"""SELECT COUNT(*) FROM ${querytable};""") { rs =>
        factorid += rs.getLong(1)
      }

      // weight variable list
      val weightlist = factorDesc.weight.variables.map(v => s""" "${v}" """).mkString(",")
      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val initvalue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }

      if (factorDesc.func.getClass.getSimpleName != "MultinomialFactorFunction") {
        // handle weights table
        // weight is fixed, or doesn't have weight variables
        if (isFixed || weightlist == ""){
          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue) 
            VALUES (${cweightid}, ${isFixed}::int, ${initvalue});""")
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"SELECT id AS factor_id, ${cweightid} AS weight_id, ${idcols} FROM ${querytable}")

          // handle weight id and factor id
          cweightid += 1
          if (!usingGreenplum) {
            executeQuery(s"SELECT nextval('${weightidSequence}');")
          }
        } else { // not fixed and has weight variables
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
            CREATE TABLE ${weighttableForThisFactor} AS
            SELECT ${weightlist}, 0::bigint AS id, ${isFixed}::int AS isfixed, ${initvalue}::real AS initvalue 
            FROM ${querytable}
            GROUP BY ${weightlist};""")

          // handle weight id
          if (usingGreenplum) {      
            executeQuery(s"""SELECT fast_seqassign('${weighttableForThisFactor.toLowerCase()}', ${cweightid});""")
          } else {
            execute(s"UPDATE ${weighttableForThisFactor} SET id = nextval('${weightidSequence}');")
          }
          issueQuery(s"""SELECT COUNT(*) FROM ${weighttableForThisFactor};""") { rs =>
            cweightid += rs.getLong(1)
          }

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue) SELECT id, isfixed, initvalue FROM ${weighttableForThisFactor};""")

          // check null weight
          val weightChecklist = factorDesc.weight.variables.map(v => s""" "${v}" IS NULL """).mkString("AND")
          issueQuery(s"SELECT COUNT(*) FROM ${querytable} WHERE ${weightChecklist}") { rs =>
            if (rs.getLong(1) > 0) {
              throw new RuntimeException("Weight variable has null values")
            }
          }

          // dump factors
          val weightjoinlist = factorDesc.weight.variables.map(v => s""" t0."${v}" = t1."${v}" """).mkString("AND")
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
             FROM ${querytable} t0, ${weighttableForThisFactor} t1
             WHERE ${weightjoinlist};""")
        }
      }

      // handle multinomial
      if (factorDesc.func.getClass.getSimpleName == "MultinomialFactorFunction") {
        // generate cardinality table for each variable
        factorDesc.func.variables.zipWithIndex.foreach { case(v,idx) =>
          val cardinalityTableName = s"${factorDesc.weightPrefix}_cardinality_${idx}"
          execute(s"""
            DROP TABLE IF EXISTS ${cardinalityTableName} CASCADE;
            SELECT * INTO ${cardinalityTableName} FROM ${v.headRelation}_${v.field}_cardinality;
            """)
        }

        // cardinality values used in weight
        val cardinalityValues = factorDesc.func.variables.zipWithIndex.map { case(v,idx) => 
          s"""_c${idx}.cardinality"""
        }
        val cardinalityTables = factorDesc.func.variables.zipWithIndex.map { case(v,idx) =>
          s"${factorDesc.weightPrefix}_cardinality_${idx} AS _c${idx}"
        }
        val cardinalityCmd = s"""${cardinalityValues.mkString(" || ',' || ")}"""

        // handle weights table
        // weight is fixed, or doesn't have weight variables
        if (isFixed || weightlist == ""){
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
            CREATE TABLE ${weighttableForThisFactor} AS
            SELECT ${cweightid} AS id, ${isFixed}::int AS isfixed, ${initvalue} AS initvalue, ${cardinalityCmd} AS cardinality
            FROM ${cardinalityTables.mkString(", ")}
            ORDER BY cardinality;""")

          // handle weight id
          if (usingGreenplum) {      
            executeQuery(s"""SELECT fast_seqassign('${weighttableForThisFactor.toLowerCase()}', ${cweightid});""")
          } else {
            execute(s"UPDATE ${weighttableForThisFactor} SET id = nextval('${weightidSequence}');")
          }

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality) 
            SELECT * FROM ${weighttableForThisFactor};""")

          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"SELECT id AS factor_id, ${cweightid} AS weight_id, ${idcols} FROM ${querytable}")

          // increment weight id
          issueQuery(s"""SELECT COUNT(*) FROM ${weighttableForThisFactor};""") { rs =>
            cweightid += rs.getLong(1)
          }
        } else { // not fixed and has weight variables
          // temporary weight table for weights without a cross product with cardinality
          val weighttableForThisFactorTemp = s"dd_weight_${factorDesc.name}_temp"
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactorTemp} CASCADE;
            CREATE TABLE ${weighttableForThisFactorTemp} AS 
            SELECT ${weightlist}, 0::bigint AS id, ${isFixed}::int AS isfixed, ${initvalue}::real AS initvalue 
            FROM ${querytable}
            GROUP BY ${weightlist};""")
          execute(s"""DROP TABLE IF EXISTS ${weighttableForThisFactor} CASCADE;
            CREATE TABLE ${weighttableForThisFactor} AS 
            SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality
            FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")}
            ORDER BY ${weightlist}, cardinality;""")

          // handle weight id
          if (usingGreenplum) {      
            executeQuery(s"""SELECT fast_seqassign('${weighttableForThisFactor.toLowerCase()}', ${cweightid});""")
          } else {
            execute(s"UPDATE ${weighttableForThisFactor} SET id = nextval('${weightidSequence}');")
          }
          issueQuery(s"""SELECT COUNT(*) FROM ${weighttableForThisFactor};""") { rs =>
            cweightid += rs.getLong(1)
          }

          execute(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality) 
            SELECT id, isfixed, initvalue, cardinality FROM ${weighttableForThisFactor};""")

          // use weight id corresponding to cardinality 0 (like C array...)
          val cardinalityKey = factorDesc.func.variables.map(v => "00000").mkString(",")

          // dump factors
          val weightjoinlist = factorDesc.weight.variables.map(v => s""" t0."${v}" = t1."${v}" """).mkString("AND")
          du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, parallelGrounding,
            s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols}
             FROM ${querytable} t0, ${weighttableForThisFactor} t1
             WHERE ${weightjoinlist} AND t1.cardinality = '${cardinalityKey}';""")
        }
      }
    }

    // dump weights
    du.unload("dd_weights", s"${groundingPath}/dd_weights",dbSettings, parallelGrounding,
      s"SELECT id, isfixed, initvalue FROM ${WeightsTable}")

    // create inference result table
    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    // split grounding files and transform to binary format
    log.info("Converting grounding file format...")
    val ossuffix = if (System.getProperty("os.name").startsWith("Linux")) "linux" else "mac"
    val cmd = s"python util/tobinary.py ${groundingPath} util/format_converter_${ossuffix} ${Context.outputDir}"
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

  //def bulkCopyWeights(weightsFile: String) : Unit
  //def bulkCopyVariables(variablesFile: String) : Unit

  def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit
  def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit

  def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
    //variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean) = {
    variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean, dbSettings: DbSettings) = {

    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    log.info("Copying inference result weights...")
    bulkCopyWeights(weightsOutputFile, dbSettings)
    log.info("Copying inference result variables...")
    bulkCopyVariables(variableOutputFile, dbSettings)
    log.info("Creating indicies on the inference result...")
    execute(createInferenceResultIndiciesSQL)

    // Each (relation, column) tuple is a variable in the plate model.
     // Find all (relation, column) combinations
    val relationsColumns = variableSchema.keys map (_.split('.')) map {
      case Array(relation, column) => (relation, column)
    }

    if (!parallelGrounding) {
      execute(createMappedWeightsViewSQL)
    }

    relationsColumns.foreach { case(relationName, columnName) =>
      execute(createInferenceViewSQL(relationName, columnName))
    //   // TODO
    //   execute(createVariableWeightsViewSQL(relationName, columnName))
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
