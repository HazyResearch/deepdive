package org.deepdive.inference

import java.io.{File, PrintWriter}
import org.deepdive.calibration._
import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import org.deepdive.settings._
import scalikejdbc._
import scala.util.matching._
import scala.io.Source
import scala.util.Random
import scala.sys.process._
import scala.util.{Try, Success, Failure}
import scala.collection.mutable.ListBuffer


/* Stores the factor graph and inference results. */
trait SQLInferenceDataStoreComponent extends InferenceDataStoreComponent {

  def inferenceDataStore : SQLInferenceDataStore

}

trait SQLInferenceDataStore extends InferenceDataStore with Logging {

  val hostname = "raiders3.stanford.edu"

  val port = 8083


  def ds : JdbcDataStore

  val factorOffset = new java.util.concurrent.atomic.AtomicLong(0)

  /* Internal Table names */
  def TempWeightsTable = "dd_temp_graph_weights"
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

  def getFactorFunctionTypeid(functionName: String) = {
    functionName match {
      case "ImplyFactorFunction" => 0
      case "OrFactorFunction" => 1
      case "AndFactorFunction" => 2
      case "EqualFactorFunction" => 3
      case "IsTrueFactorFunction" =>  4
      case "ConvolutionFactorFunction" => 1000
      case "SamplingFactorFunction" => 1001
      case "HiddenFactorFunction" => 1005
      case "LikelihoodFactorFunction" => 1010
      case "LeastSquaresFactorFunction" => 1011
      case "SoftmaxFactorFunction" => 1020
    }
  }

  /* Executes an arbitary SQL statement */
  def executeSql(sql: String) = ds.DB.autoCommit { implicit session =>
    """;\s+""".r.split(sql.trim()).filterNot(_.isEmpty).map(_.trim).foreach { 
        query => SQL(query).execute.apply()
    }
  }

  def execute(sql: String) {
    try {
      executeSql(sql)
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
        log.error(exception.toString)
        log.info("[Error] Please check the SQL cmd!")
        throw exception
    }
  }

  /* Issues a query */
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {

    val conn = ds.borrowConnection()
    conn.setAutoCommit(false);
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(5000);
    val rs = stmt.executeQuery(sql)
    while(rs.next()){
      op(rs)
    }
    conn.close()
  }


  def selectAsMap(sql: String) : List[Map[String, Any]] = {
    ds.DB.readOnly { implicit session =>
      SQL(sql).map(_.toMap).list.apply()
    }
  }

  def keyType = "bigserial"
  def stringType = "text"
  def randomFunc = "RANDOM()"


  def checkGreenplumSQL = s"""
    SELECT version() LIKE '%Greenplum%';
  """

  def createTempWeightsSQL = s"""
    DROP TABLE IF EXISTS ${TempWeightsTable} CASCADE;
    CREATE TABLE ${TempWeightsTable}(
      id bigserial primary key,
      initial_value double precision,
      is_fixed boolean,
      description text,
      weight_lenght int);
    ALTER SEQUENCE ${TempWeightsTable}_id_seq MINVALUE -1 RESTART WITH 0;
  """

  def createWeightsSQL = s"""
    DROP TABLE IF EXISTS ${WeightsTable} CASCADE;
    CREATE TABLE ${WeightsTable}(
      id bigserial primary key,
      initial_value double precision,
      is_fixed boolean,
      description text,
      weight_lenght int);
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
    SELECT id AS "id", is_fixed AS "is_fixed", initial_value AS "initial_value", description AS "description", weight_lenght AS "weight_lenght"
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

  // TODO: Add for Real numbers!?!
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

  //TODO:change weightVariables 
  // CASE WHEN conv_layer0-"feature" IS NULL THEN '' ELSE "feature"::text END || ... 
  def generateWeightCmd(weightPrefix: String, weightVariables: Seq[String]) : String = 
    weightVariables.map ( v => s"""(CASE WHEN "${v}" IS NULL THEN '' ELSE "${v}"::text END)""" )
      .mkString(" || ") match {
      case "" => s"""'${weightPrefix}-' """
      case x => s"""'${weightPrefix}-' || ${x}"""
  }
  def executeQuery(sql: String) = {

    log.info("EXECUTING.... " + sql)
    val conn = ds.borrowConnection()
    conn.setAutoCommit(false)
    val stmt = conn.createStatement();

    /*
    try {
      val rsa = stmt.executeQuery("explain " + sql);
      while (rsa.next())
      {
        log.info("USING " + rsa.getString(1));
      }
      rsa.close();
    } catch {
      case exception : Throwable =>
        log.error(exception.toString)
    }
    */

    stmt.execute(sql)
    conn.commit()
    conn.close()
    log.info("      DONE :-)! ")
  }

  def queryCount(sql: String) : Long = {

    log.info("EXECUTING.... " + sql)
    val conn = ds.borrowConnection()
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(10000);
    val rs = stmt.executeQuery(sql)
    while(rs.next()){
      val a = rs.getLong(1)
      conn.close()
      log.info("      DONE :-)! COUNT = " + a)
      return a
    }
    conn.close()
    log.info("      FAILED :-(! ")
    return -1
  }


// . Generate grounding files for sampler
//    . Variable, weight, factor, edge, and meta data files
  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String],
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String) : Unit = {
  }

// Only deals with weight table 
//  . Assign ID to the table contains variables
//  . Assign Holdout ( marking the evidences for cross validation)
//  . Create view for input query
//  . Create weight table 

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String, dbSettings: DbSettings) {

   executeQuery(createAssignIdFunctionSQL)
   var id : Long = 0
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      executeQuery(s"""SELECT fast_seqassign('${relation}', ${id});""")
      id = 1+ queryCount(s"""SELECT max(id) FROM ${relation}""")

    }

    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      val variableDataType = dataType.toString match {
        case "Boolean" => 0
        case "Multinomial" => 1
        case "Real" =>2
        case "ArrayReal" => 3
      }
      
      executeQuery(s"""
        DROP EXTERNAL TABLE IF EXISTS variables_${relation} CASCADE;
        CREATE WRITABLE EXTERNAL TABLE variables_${relation} (
          id bigint,
          num_rows bigint,
          num_cols bigint,
          is_evidence int[],
          initial_value double precision[],
          layer bigint) 
        LOCATION ('gpfdist://${hostname}:${port}/variables_${relation}')
        FORMAT 'TEXT';
        """)

      executeQuery(s"""
        INSERT INTO variables_${relation}(id, num_rows, num_cols, is_evidence, initial_value, layer)
        (SELECT id, num_rows, num_cols, ${column}, ${column}, layer
          FROM ${relation})
        """)

    }
    

    executeQuery(s"""DROP EXTERNAL TABLE IF EXISTS edges CASCADE;""")
    executeQuery(s"""CREATE WRITABLE EXTERNAL TABLE edges (in_ids bigint[], in_locations_x int[], in_locations_y int[], 
                      out_id int, out_location_x int, out_location_y int, num_ids int, function_name int, weight_ids bigint[])
                   LOCATION ('gpfdist://${hostname}:${port}/edges')
                     FORMAT 'TEXT';
      """)
    
    executeQuery(s"""DROP EXTERNAL TABLE IF EXISTS weights CASCADE;""")
    executeQuery(s"""CREATE WRITABLE EXTERNAL TABLE weights (id bigint, num_rows int, num_cols int, isfixed int, initvalue float) 
                        LOCATION ('gpfdist://${hostname}:${port}/weights')
                        FORMAT 'TEXT';
                  """)

    var cweightid = 0L
    factorDescs.zipWithIndex.foreach { case(factorDesc, idx) =>

      val selectcols = factorDesc.func.variables.zipWithIndex.map { case(variable, position) =>
        val vRelation = variable.headRelation
        val vColumn = variable.field
        val vidColumn = s""" "${variable.relation}.id" """
        vidColumn
      }

      val selectcol = selectcols.mkString(" , ")

      executeQuery(s"""DROP TABLE IF EXISTS queryview_${factorDesc.name} CASCADE;""")
      executeQuery(s"""DROP TABLE IF EXISTS weighttable_${factorDesc.name} CASCADE;""")

      executeQuery(s"""CREATE TABLE queryview_${factorDesc.name} AS ${factorDesc.inputQuery};""")

      var pos=0;
      var is_known=0;
      val functionName = getFactorFunctionTypeid(factorDesc.func.getClass.getSimpleName)
      var cweightids=new ListBuffer[Long]()
      var weightjoinlist=""
      var weightlist="" 
      var break=1;
      factorDesc.weight.weightList.foreach { case(weightobj) =>
        if(break==1 && weightobj.isInstanceOf[KnownFactorWeight] == false ){
          var weightLength = Math.sqrt(weightobj.vectorLength.toDouble)
          var isfixed = 0
          var initvalue = 0
          weightlist = weightobj.variables.map ( v => s"""${v}""" ).mkString(" , ")
          // log.info("~~~~~~~~~" + weightLength.toString);
          // log.info("~~~~~~~~~" + weightlist);
          if(weightlist!="")
            executeQuery(s"""CREATE TABLE  weighttable_${factorDesc.name} AS 
                            (SELECT ${weightlist}, 0::bigint id, ${weightLength}::int num_rows, ${weightLength}::int num_cols, ${isfixed}::int isfixed, ${initvalue}::float initvalue, 0::int pos
                            FROM queryview_${factorDesc.name} 
                            GROUP BY ${weightlist} limit 0)
                            DISTRIBUTED BY (${weightlist});
                          """)
          break=0;
        }
      }

      factorDesc.weight.weightList.foreach { case(weightobj) =>
        // log.info("+++++ weightobj: " + weightobj.toString)
        var isfixed = 0
        if(weightobj.isInstanceOf[KnownFactorWeight] || weightobj.isInstanceOf[KnownFactorWeightVector]){
          isfixed = 1
        }
        val initvalue = weightobj match { 
          case x : KnownFactorWeight => x.value
          case x : KnownFactorWeightVector => x.value
          case _ => 0.0
        }
        var weightLength = Math.sqrt(weightobj.vectorLength.toDouble)
        // log.info("+++++ weightlist: " + weightlist.toString)
        if(weightobj.isInstanceOf[KnownFactorWeight] == true || weightlist == ""){
          // log.info("~~~~~~~~~" + "KNOWN FACTOR WEIGHT");
          is_known=1
          executeQuery(s"""INSERT INTO weights VALUES (${cweightid}, ${weightLength}, ${weightLength}, ${isfixed}, ${initvalue});""")   
          cweightids+=cweightid
          cweightid = cweightid + 1
          // log.info("~~~~~~cweightid: "+ cweightid.toString)
        }else{
          if(is_known==1){
            Failure(new RuntimeException(s"Script exited because of Bad Weights FORMAT!!!!!!"))
          }
          // log.info("~~~~~~~~~" + "UNKNOWN FACTOR WEIGHT");
          var isfixed = 0
          val initvalue = 0
          weightjoinlist = weightobj.variables.map ( v => s"""t0.${v}=t1.${v}""" ).mkString(" AND ")
          executeQuery(s"""INSERT INTO weighttable_${factorDesc.name} 
                      (SELECT ${weightlist}, 0::bigint id, ${weightLength}::int num_rows, ${weightLength}::int num_cols, ${isfixed}::int isfixed, ${initvalue}::float initvalue, ${pos}::int pos
                      FROM queryview_${factorDesc.name} 
                      GROUP BY ${weightlist});""") 
          executeQuery(s"""SELECT fast_seqassign('weighttable_${factorDesc.name}', ${cweightid});""")      
        }
        pos+=1;
      }
      if(is_known==1){
        val cweightids_str="{"+cweightids.toString.split('(')(1).split(')')(0)+"}"
        // val cweightids_str="{"+cweightid.toString+"}"
        executeQuery(s"""INSERT INTO edges 
                      (SELECT t0.ids AS in_ids, t0.locations_x AS in_locations_x, t0.locations_y AS in_locations_y,  
                            t0.id AS out_id, t0.location_x AS out_location_x, t0.location_y AS out_location_y, t0.num_ids AS num_ids,
                            '${functionName}', '${cweightids_str}'::bigint[] AS weights
                      FROM queryview_${factorDesc.name} t0);
                  """)
      }
      else{
        executeQuery(s"""INSERT INTO edges 
                (SELECT t0.ids AS in_ids, t0.locations_x AS in_locations_x, t0.locations_y AS in_locations_y,  
                     t0.id AS out_id, t0.location_x AS out_location_x, t0.location_y AS out_location_y, t0.num_ids AS num_ids,
                     '${functionName}' AS function_name, array_agg(t1.id ORDER BY t1.pos) AS weight_ids
                FROM queryview_${factorDesc.name} t0, weighttable_${factorDesc.name} t1
                WHERE ${weightjoinlist}
                GROUP BY t0.ids,t0.locations_x,t0.locations_y,t0.id,t0.location_x,t0.location_y, t0.num_ids);""")
        executeQuery(s"""INSERT INTO weights (SELECT id, num_rows, num_cols, isfixed, initvalue FROM weighttable_${factorDesc.name});""")
        cweightid = cweightid + queryCount(s"""SELECT COUNT(*) FROM weighttable_${factorDesc.name};""")
      }
    }
    

    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)
  }

  def bulkCopyWeights(weightsFile: String) : Unit
  def bulkCopyVariables(variablesFile: String) : Unit

  def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
    variableOutputFile: String, weightsOutputFile: String) = {

    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)
    // execute(createMappedInferenceResultView)

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
      // TODO
      case _ =>
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