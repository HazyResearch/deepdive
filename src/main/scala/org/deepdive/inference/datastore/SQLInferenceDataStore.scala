package org.deepdive.inference

import java.io.{File, PrintWriter}
import org.deepdive.calibration._
import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
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
  def IdSequence = "id_sequence"


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

  def execute(sql: String) {
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

  /* Issues a query */
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {
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
  }


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

  def checkGreenplumSQL = s"""
    SELECT version() LIKE '%Greenplum%';
  """

  def createWeightsSQL = s"""
    DROP TABLE IF EXISTS ${WeightsTable} CASCADE;
    CREATE TABLE ${WeightsTable}(
      id bigserial primary key,
      initial_value double precision,
      is_fixed boolean,
      description text,
      count bigint);
    ALTER SEQUENCE ${WeightsTable}_id_seq MINVALUE -1 RESTART WITH 0;
  """

  def copyLastWeightsSQL = s"""
    DROP TABLE IF EXISTS ${lastWeightsTable} CASCADE;
    SELECT X.*, Y.weight INTO ${lastWeightsTable}
      FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id
      ORDER BY id ASC;
  """

  def createVariablesHoldoutSQL = s"""
    DROP TABLE IF EXISTS ${VariablesHoldoutTable} CASCADE; 
    CREATE TABLE ${VariablesHoldoutTable}(
      variable_id bigint primary key);
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

  def selectWeightsForDumpSQL = s"""
    SELECT id AS "id", is_fixed AS "is_fixed", initial_value AS "initial_value", description AS "description"
    FROM ${WeightsTable};
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

  def generateWeightCmd(weightPrefix: String, weightVariables: Seq[String]) : String = 
    weightVariables.map ( v => s"""(CASE WHEN "${v}" IS NULL THEN '' ELSE "${v}"::text END)""" )
      .mkString(" || ") match {
      case "" => s"""'${weightPrefix}-' """
      case x => s"""'${weightPrefix}-' || ${x}"""
  }

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String, 
    dbSettings: DbSettings, parallelGrounding: Boolean) {

    // parallel grounding
    if (parallelGrounding) {
      groundFactorGraphParallel(schema, factorDescs, holdoutFraction, holdoutQuery, skipLearning, weightTable, dbSettings)
      return
    }

    // We write the grounding queries to this SQL file
    val sqlFile = File.createTempFile(s"grounding", ".sql")
    val writer = new PrintWriter(sqlFile)
    log.info(s"""Writing grounding queries to file="${sqlFile}" """)

    // If skip_learning and use the last weight table, copy it before removing it
    if (skipLearning && weightTable.isEmpty()) {
      writer.println(copyLastWeightsSQL)
    }

    writer.println(createWeightsSQL)
    writer.println(createVariablesHoldoutSQL)

    // check whether Greenplum is used
    var usingGreenplum = false
    issueQuery(checkGreenplumSQL) { rs => 
      usingGreenplum = rs.getBoolean(1) 
    }

    log.info("Using Greenplum = " + usingGreenplum.toString)

    // assign id
    if (usingGreenplum) {
      executeQuery(createAssignIdFunctionSQL)
    } else {
      writer.println(createSequencesSQL)
    }
    var idoffset : Long = 0

    // Ground all variables in the schema
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      if (usingGreenplum) {
        executeQuery(s"""SELECT fast_seqassign('${relation}', ${idoffset});""")
        issueQuery(s"""SELECT max(id) FROM ${relation}""") { rs =>
          idoffset = 1 + rs.getLong(1)
        }
      } else {
        writer.println(s"""
          UPDATE ${relation} SET id = nextval('${IdSequence}');
          """)
      }
       
    }

    // Assign the holdout - Random (default) or user-defined query
    holdoutQuery match {   
      case Some(userQuery) => writer.println(userQuery + ";")
      case None =>
    }

    // Create table for each inference rule
    factorDescs.foreach { factorDesc =>
      
      // input query
      writer.println(s"""
        DROP VIEW IF EXISTS ${factorDesc.name}_query_user CASCADE;
        CREATE VIEW ${factorDesc.name}_query_user AS ${factorDesc.inputQuery};
        """)


      // Ground weights for each inference rule
      val weightValue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }

      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables)

      writer.println(s"""
        INSERT INTO ${WeightsTable}(initial_value, is_fixed, description, count)
        SELECT ${weightValue} AS wValue, ${isFixed} AS wIsFixed, ${weightCmd} AS wCmd, COUNT(${weightValue})
        FROM ${factorDesc.name}_query_user GROUP BY wValue, wIsFixed, wCmd;""")
    }

    // skip learning: choose a table to copy weights from
    if (skipLearning) {
      val fromWeightTable = weightTable.isEmpty() match {
        case true => lastWeightsTable
        case false => weightTable
      }
      log.info(s"""Using weights in TABLE ${fromWeightTable} by matching description""")

      // Already set -l 0 for sampler
      writer.println(s"""
        UPDATE ${WeightsTable} SET initial_value = weight 
        FROM ${fromWeightTable} 
        WHERE dd_graph_weights.description = ${fromWeightTable}.description;
        """)
    }

    writer.println(s"""CREATE INDEX ${WeightsTable}_desc_idx ON ${WeightsTable}(description);""")
    writer.println(s"""ANALYZE ${WeightsTable};""")

    writer.close()

    log.info("Executing grounding query...")
    execute(Source.fromFile(sqlFile).getLines.mkString)
  }

  def groundFactorGraphParallel(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String, dbSettings: DbSettings) {

    // clean up grounding folder
    val hostname = dbSettings.gphost
    val port = dbSettings.gpport
    val gppath = dbSettings.gppath
    if (hostname == "" || port == "" || gppath == "") {
      throw new RuntimeException("Missing GPFDIST Settings.")
    }

    
    log.info(s"EXECUTING: rm -rf ${gppath}/*")
    s"rm -rf ${gppath}/*".!

    // assign variable ids
    executeQuery(createAssignIdFunctionSQL)
    var id : Long = 0
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      executeQuery(s"""SELECT fast_seqassign('${relation}', ${id});""")
      issueQuery(s"""SELECT max(id) FROM ${relation}""") { rs =>
        id = 1 + rs.getLong(1)
      }
    }

    // ground variables
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      val variableDataType = dataType.toString match {
        case "Boolean" => 0
        case "Multinomial" => 1
      }

      execute(s"""
        DROP TABLE IF EXISTS ${VariablesHoldoutTable} CASCADE; 
        CREATE TABLE ${VariablesHoldoutTable}(variable_id bigint primary key);
        """)
      execute(s"""
        INSERT INTO ${VariablesHoldoutTable}
        SELECT id FROM ${relation}
        WHERE RANDOM() > ${holdoutFraction} AND ${column} IS NOT NULL;
        """)
      
      execute(s"""
        DROP EXTERNAL TABLE IF EXISTS variables_${relation} CASCADE;
        CREATE WRITABLE EXTERNAL TABLE variables_${relation} (
          id bigint,
          is_evidence int,
          initial_value double precision,
          type int,
          cardinality int) 
        LOCATION ('gpfdist://${hostname}:${port}/variables_${relation}')
        FORMAT 'TEXT';
        """)

      execute(s"""
        INSERT INTO variables_${relation}(id, is_evidence, initial_value, type, cardinality)
        (SELECT id, (${VariablesHoldoutTable}.variable_id IS NOT NULL)::int, 
          COALESCE(${column}::int,0), ${variableDataType}, 1 
        FROM ${relation} LEFT OUTER JOIN ${VariablesHoldoutTable} ON ${relation}.id = ${VariablesHoldoutTable}.variable_id)
        """)
    }
    

    // generate factor meta data
    execute(s"""DROP EXTERNAL TABLE IF EXISTS factormeta CASCADE;""")
    execute(s"""CREATE WRITABLE EXTERNAL TABLE factormeta (name text, funcid text, sign text)
      LOCATION ('gpfdist://${hostname}:${port}/factormeta')
      FORMAT 'TEXT';
      """)

    factorDescs.foreach { factorDesc =>
        var s = ""
        val negs = factorDesc.func.variables.zipWithIndex.foreach{ case(variable, position) =>
        val isPositive = !variable.isNegated
        s = s + " " + isPositive
       }
    

       val functionName = factorDesc.func.getClass.getSimpleName
       execute(s"""INSERT INTO factormeta VALUES ('${factorDesc.name}', '${functionName}', '${s}');""")
    }
    
    // weights
    execute(s"""DROP EXTERNAL TABLE IF EXISTS weights CASCADE;""")
    execute(s"""CREATE WRITABLE EXTERNAL TABLE weights (id bigint, isfixed int, initvalue float) 
      LOCATION ('gpfdist://${hostname}:${port}/weights')
      FORMAT 'TEXT';
      """)

    var cweightid = 0L
    factorDescs.zipWithIndex.foreach { case (factorDesc, idx) =>
      // id columns
      val selectcols = factorDesc.func.variables.zipWithIndex.map { case (variable, position) =>
        val vRelation = variable.headRelation
        val vColumn = variable.field
        val vidColumn = s""" "${variable.relation}.id" """
        vidColumn
      }

      val selectcol = selectcols.mkString(" , ")

      execute(s"""DROP TABLE IF EXISTS queryview_${factorDesc.name} CASCADE;""")
      execute(s"""DROP TABLE IF EXISTS weighttable_${factorDesc.name} CASCADE;""")
      execute(s"""DROP TABLE IF EXISTS query_readytogo_${factorDesc.name} CASCADE;""")

      // table of input query
      execute(s"""CREATE TABLE queryview_${factorDesc.name} AS ${factorDesc.inputQuery};""")

      // weight variable list
      val weightlist = factorDesc.weight.variables.map ( v => s"""${v}""" ).mkString(" , ")
      var isfixed = 0
      if(factorDesc.weight.isInstanceOf[KnownFactorWeight]){
        isfixed = 1
      }
      val initvalue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }

      // ground weights
      if (factorDesc.weight.isInstanceOf[KnownFactorWeight] == true || weightlist == ""){
        execute(s"""INSERT INTO weights VALUES (${cweightid}, ${isfixed}, ${initvalue});""")
        execute(s"""CREATE TABLE query_readytogo_${factorDesc.name} AS
          SELECT ${cweightid}::bigint as _weight_id, ${selectcol}
          FROM queryview_${factorDesc.name};
          """)
        cweightid = cweightid + 1
      } else {
        var isfixed = 0
        val initvalue = 0
        val weightjoinlist = factorDesc.weight.variables.map ( v => s""" t0."${v}" = t1."${v}" """ ).mkString(" AND ")
        execute(s"""CREATE TABLE weighttable_${factorDesc.name} AS
          SELECT "${weightlist}", 0::bigint id, ${isfixed}::int isfixed, ${initvalue}::float initvalue 
          FROM queryview_${factorDesc.name} 
          GROUP BY "${weightlist}"
          DISTRIBUTED BY ("${weightlist}");""")
        executeQuery(s"""SELECT fast_seqassign('weighttable_${factorDesc.name}', ${cweightid});""")
        execute(s"""CREATE TABLE query_readytogo_${factorDesc.name} AS
          SELECT t1.id as _weight_id, ${selectcol}
          FROM queryview_${factorDesc.name} t0, weighttable_${factorDesc.name} t1
          WHERE ${weightjoinlist};
          """)

        execute(s"""INSERT INTO weights (SELECT id, isfixed, initvalue FROM weighttable_${factorDesc.name});""")

        issueQuery(s"""SELECT COUNT(*) FROM weighttable_${factorDesc.name};""") { rs =>
          cweightid += rs.getLong(1)
        }
      }
    }

    // ground factors
    factorDescs.zipWithIndex.foreach { case (factorDesc, idx) =>

        val selectcols = factorDesc.func.variables.zipWithIndex.map { case (variable, position) =>
          val vRelation = variable.headRelation
          val vColumn = variable.field
          val vidColumn = s""" v${position} bigint """
          vidColumn
        }

        val selectcol = selectcols.mkString(" , ")
        val definition = "_weight_id bigint, " + selectcol

        //TODO: NEED TO REMOVE
        execute(s"""DROP EXTERNAL TABLE IF EXISTS factors_${factorDesc.name}_out CASCADE;""")
        execute(s"""CREATE WRITABLE EXTERNAL TABLE factors_${factorDesc.name}_out (${definition}) 
          LOCATION ('gpfdist://${hostname}:${port}/factors_${factorDesc.name}_out')
          FORMAT 'TEXT';
          """)

        execute(s"""INSERT INTO factors_${factorDesc.name}_out
          (SELECT * FROM query_readytogo_${factorDesc.name});
          """)
    }
    
    // create inference result table
    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    // split grounding files and transform to binary format
    log.info("Converting grounding file format...")
    val cmd = s"python util/tobinary.py ${gppath} util/convert_format"
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

    if (parallelGrounding) {
      return
    }

    var numVariables  : Long = 0
    var numFactors    : Long = 0
    var numWeights    : Long = 0
    var numEdges      : Long = 0

    val randomGen = new Random()
    val weightMap = scala.collection.mutable.Map[String, Long]()

    val customHoldout = holdoutQuery match {
      case Some(query) => true
      case None => false
    }
    
    log.info(s"Dumping factor graph...")
    log.info("Dumping variables...")

    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      
      val selectVariablesForDumpSQL = s"""
        SELECT ${relation}.id, (${variable} IS NOT NULL), ${variable}::int, (${VariablesHoldoutTable}.variable_id IS NOT NULL)
        FROM ${relation} LEFT JOIN ${VariablesHoldoutTable}
        ON ${relation}.id = ${VariablesHoldoutTable}.variable_id"""

      val cardinality = dataType match {
        case BooleanType => 0
        case MultinomialType(x) => x.toLong
      }

      issueQuery(selectVariablesForDumpSQL) { rs =>
        var isEvidence = rs.getBoolean(2)
        var holdout = rs.getBoolean(4)

        // assign holdout
        if (customHoldout && isEvidence && holdout) {
          isEvidence = false
        } else if (!customHoldout && isEvidence && randomGen.nextFloat < holdoutFraction) {
          isEvidence = false
        }

        serializer.addVariable(
          rs.getLong(1),            // id
          isEvidence,               // is evidence
          rs.getLong(3),            // initial value
          dataType.toString,        // data type            
          -1,                       // edge count
          cardinality)              // cardinality
        numVariables += 1
      }

    }

    log.info("Dumping weights...")
    issueQuery(selectWeightsForDumpSQL) { rs => 
      val id = rs.getLong(1)
      serializer.addWeight(id, rs.getBoolean(2), rs.getDouble(3))
      numWeights += 1
      weightMap(rs.getString(4)) = id
    }


    log.info("Dumping factors...")
    factorDescs.foreach { factorDesc =>
      val functionName = factorDesc.func.getClass.getSimpleName
      
      log.info(s"Dumping inference ${factorDesc.weightPrefix}...")

      val selectInputQueryForDumpSQL = s"""
        SELECT ${factorDesc.name}_query_user.*
        FROM ${factorDesc.name}_query_user
        """

      val variables = factorDesc.func.variables

      issueQuery(selectInputQueryForDumpSQL) { rs =>

        val weightCmd = factorDesc.weightPrefix + "-" + factorDesc.weight.variables.map(
        v => rs.getString(v)).mkString("")

        serializer.addFactor(numFactors, weightMap(weightCmd), functionName, variables.length)

        variables.zipWithIndex.foreach { case(v, pos) =>
          serializer.addEdge(rs.getLong(s"${v.relation}.id"),
            numFactors, pos, !v.isNegated, v.predicate.getOrElse(1))
          numEdges += 1
        }

        numFactors += 1
      }

    }

    serializer.writeMetadata(numWeights, numVariables, numFactors, numEdges,
      weightsPath, variablesPath, factorsPath, edgesPath)

    serializer.close()
  }

  def bulkCopyWeights(weightsFile: String) : Unit
  def bulkCopyVariables(variablesFile: String) : Unit

  def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
    variableOutputFile: String, weightsOutputFile: String) = {

    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)

    log.info("Copying inference result weights...")
    bulkCopyWeights(weightsOutputFile)
    log.info("Copying inference result variables...")
    bulkCopyVariables(variableOutputFile)
    log.info("Creating indicies on the inference result...")
    execute(createInferenceResultIndiciesSQL)

    // Each (relation, column) tuple is a variable in the plate model.
     // Find all (relation, column) combinations
    val relationsColumns = variableSchema.keys map (_.split('.')) map {
      case Array(relation, column) => (relation, column)
    }

    execute(createMappedWeightsViewSQL)

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
