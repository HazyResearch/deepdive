package org.deepdive.inference

import java.io.{File, PrintWriter}
import org.deepdive.calibration._
import org.deepdive.datastore.JdbcDataStore
import scala.collection.mutable.ArrayBuffer
import org.deepdive.Logging
import org.deepdive.settings._
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
    \$\$
    if '__count_1' in SD:
      SD['__count_1'] = -1
      return 1
    return 0
    \$\$ LANGUAGE plpythonu;
     
     
    CREATE OR REPLACE FUNCTION updateid(startid bigint, sid int, sids int[], base_ids bigint[], base_ids_noagg bigint[]) RETURNS bigint AS 
    \$\$
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
      
    \$\$ LANGUAGE plpythonu;
     
    CREATE OR REPLACE FUNCTION fast_seqassign(tname character varying, startid bigint) RETURNS TEXT AS 
    \$\$
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
    \$\$ LANGUAGE 'plpgsql';
     
    CREATE OR REPLACE FUNCTION _fast_seqassign(tname character varying, startid bigint)
    RETURNS TEXT AS
    \$\$
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
    \$\$
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

// . Generate grounding files for sampler
//    . Variable, weight, factor, edge, and meta data files
  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String],
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String) : Unit = {

// COUNTERS for meta data files
    var numVariables  : Long = 0
    var numFactors    : Long = 0
    var numWeights    : Long = 0
    var numEdges      : Long = 0

    val randomGen = new Random()

    // its mapping from weight discreption to weight ID
    val weightMap = scala.collection.mutable.Map[String, Long]()
    val variableMap = scala.collection.mutable.Map[(Long,Long), Long]()


    val customHoldout = holdoutQuery match {
      case Some(query) => true
      case None => false
    }
    
    log.info(s"Dumping factor graph...")
    

    log.info("Dumping variables...")
    schema.foreach { case(variable, dataType) => //(variables0_layer1.value,Real) or (variables1_layer0.prev_id,ArrayReal)
      val Array(relation, column) = variable.split('.') //(variables0_layer1,value) or (variables1_layer0,prev_id)
      // The selection is based on the serializer function used bellow (serializer.addVariable())

      val cardinality = dataType match {
        case BooleanType => 0
        case MultinomialType(x) => x.toLong
        case RealType => 0
        case ArrayRealType  => 0
        // TODO
      }

      val isArray = dataType match {
        case ArrayRealType  => 1
        case _ => 0
        // TODO
      }
      var selectVariablesForDumpSQL = "";
      if (isArray==0){
        selectVariablesForDumpSQL = s"""
          SELECT ${relation}.id, (${variable} IS NOT NULL), ${variable}::real, (${VariablesHoldoutTable}.variable_id IS NOT NULL)
          FROM ${relation} LEFT JOIN ${VariablesHoldoutTable}
          ON ${relation}.id = ${VariablesHoldoutTable}.variable_id"""
        issueQuery(selectVariablesForDumpSQL) { rs =>
          var isEvidence = rs.getBoolean(2)
          // log.info(isEvidence.toString)

          var holdout = rs.getBoolean(4)

          // assign holdout
          if (customHoldout && isEvidence && holdout) {
            isEvidence = false
          } else if (!customHoldout && isEvidence && randomGen.nextFloat < holdoutFraction) {
            isEvidence = false
          }
          val id = rs.getLong(1)
          // log.info("Variable : " +(id).toString+","+isEvidence.toString+","+
          //   rs.getDouble(3).toString+","+dataType.toString())

          serializer.addVariable(
            id,                      // id
            isEvidence,               // is evidence
            rs.getDouble(3))          // initial value

          numVariables += 1
          //}                                                 
        }
      }
      else{
        // log.info("ArrayRealType")
        selectVariablesForDumpSQL = s"""
          SELECT ${relation}.id, ${variable}::real[], (${VariablesHoldoutTable}.variable_id IS NOT NULL)
          FROM ${relation} LEFT JOIN ${VariablesHoldoutTable}
          ON ${relation}.id = ${VariablesHoldoutTable}.variable_id"""
        issueQuery(selectVariablesForDumpSQL) { rs =>
          var ind=0
          for( vari <- (rs.getArray(2)).toString.split(",|\\}|\\{"); if vari!=""){
            // log.info("var_id = " + var_id.toString)
            // log.info("ind = " + ind.toString)
            var isEvidence=false
            var vari_double=0.0
            if(vari=="NULL")
              isEvidence=true
            else
              vari_double=vari.toDouble


            var holdout = rs.getBoolean(3)

            // assign holdout
            if (customHoldout && isEvidence && holdout) {
              isEvidence = false
            } else if (!customHoldout && isEvidence && randomGen.nextFloat < holdoutFraction) {
              isEvidence = false
            }
            val mat_id= rs.getLong(1)
            val id = numVariables;
            variableMap((mat_id,ind)) = numVariables;
            // log.info("Variable : " +(id).toString+","+isEvidence.toString+","+
            //   rs.getDouble(3).toString+","+dataType.toString())

            serializer.addVariable(
              id,                      // id
              isEvidence,               // is evidence
              vari_double)                     // initial value

            numVariables += 1
            ind=ind+1
          }
        }
      }
      // log.info("num_variables : " + numVariables.toString)
      // log.info(s"""${variable}""")

      

    }

    log.info("Dumping weights...")
    // initializing wightMap 
    issueQuery(selectWeightsForDumpSQL) { rs => 
      val id = rs.getLong(1)
      // Write to the weights file
      // log.info("Weight : " +(id).toString+","+rs.getBoolean(2).toString+","+rs.getDouble(3).toString)
      serializer.addWeight(id, rs.getBoolean(2), rs.getDouble(3),rs.getLong(5))
      numWeights += 1
      weightMap(rs.getString(4)) = id
      // log.info(rs.getString(4))
    }


    log.info("Dumping factors...")
    factorDescs.foreach { factorDesc =>

      
      log.info(s"Dumping inference ${factorDesc.weightPrefix}...")

      val functionName = factorDesc.func.getClass.getSimpleName
      log.info("functionName = " + functionName.toString)
      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight] | factorDesc.weight.isInstanceOf[KnownFactorWeightVector]
      val selectInputQueryForDumpSQL = s"""
        SELECT ${factorDesc.name}_query_user.*
        FROM ${factorDesc.name}_query_user
        """
      val variables = factorDesc.func.variables


      log.info("****************************************************************")
      issueQuery(selectInputQueryForDumpSQL) { rs =>

        var weightCmd = factorDesc.weightPrefix + "-" + factorDesc.weight.variables.map(
        v => rs.getString(v)).mkString("")
        var weightLength = factorDesc.weight.vectorLength

        if(weightLength==0){          // log.info("factorDesc.weight.vectorLength==0")
          serializer.addFactor(numFactors, functionName)
        }
        else{
          if(isFixed){
            serializer.addFactor(numFactors, functionName)
          }else{
            serializer.addFactor(numFactors, functionName)
          }
        }

        if(weightLength==0){          // log.info("factorDesc.weight.vectorLength==0")
          variables.zipWithIndex.foreach { case(v, pos) =>
            serializer.addEdge(variableMap(rs.getLong(s"${v.relation}.id"),0), numFactors, weightMap(weightCmd), pos)
            numEdges += 1
          }
        }else{
          variables.zipWithIndex.foreach { case(v, pos) =>
            if(pos==0){ //Vector variables
              // log.info("---------------------------------------------------- " + (rs.getArray(s"${v.relation}.ids")).toString)
              var ind=1
              var ids=ArrayBuffer[Long]()
              var locations=ArrayBuffer[Long]()
              for(var_id <- (rs.getArray(s"${v.relation}.ids")).toString.split(",|\\}|\\{") if var_id!="")
                ids+=var_id.toLong
              for(loc <- (rs.getArray(s"${v.relation}.locations")).toString.split(",|\\}|\\{") if loc!="")
                locations+=loc.toLong

              for( i <- 0 to ids.length-1){
                // log.info("var_id = " + var_id.toString)
                // log.info("ind = " + ind.toString)
                if(isFixed){
                  serializer.addEdge(variableMap(ids(i),locations(i)), numFactors, weightMap(weightCmd), pos)
                  numEdges += 1
                }else{
                  weightCmd = factorDesc.weightPrefix + "-" + factorDesc.weight.variables.map(
                     v => rs.getString(v)).mkString("") + "-" + ind.toString
                  serializer.addEdge(variableMap(ids(i),locations(i)), numFactors, weightMap(weightCmd), pos)
              //    log.info(var_id+","+numFactors.toString+","+pos.toString+","+weightMap(weightCmd))
                  numEdges += 1
                  ind+=1
                }
              }  
            }else if(pos==2){ // Output of the factor
             // log.info((rs.getLong(s"${v.relation}.id")).toString+","+numFactors.toString+","+pos.toString)
              serializer.addEdge(variableMap(rs.getLong(s"${v.relation}.id"),rs.getLong(s"${v.relation}.location")), numFactors, -1, pos)
              numEdges += 1
            }
          }
        }
        numFactors += 1
      }
                                log.info("----------------------------------------------------")
      // log.info("("+numVariables.toString  +","+ numFactors.toString +","+  numWeights.toString +"," + numEdges.toString +")" )
    }


    serializer.writeMetadata(numWeights, numVariables, numFactors, numEdges,
      weightsPath, variablesPath, factorsPath, edgesPath)

    serializer.close()
  }

// Only deals with weight table 
//  . Assign ID to the table contains variables
//  . Assign Holdout ( marking the evidences for cross validation)
//  . Create view for input query
//  . Create weight table 

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String, dbSettings: DbSettings) {

    // Get Database-related settings
    val dbname = dbSettings.dbname
    val pguser = dbSettings.user
    val pgport = dbSettings.port
    val pghost = dbSettings.host
    // TODO do not use password for now
    val dbnameStr = dbname match {
      case null => ""
      case _ => s" -d ${dbname} "
    }
    val pguserStr = pguser match {
      case null => ""
      case _ => s" -U ${pguser} "
    }
    val pgportStr = pgport match {
      case null => ""
      case _ => s" -p ${pgport} "
    }
    val pghostStr = pghost match {
      case null => ""
      case _ => s" -h ${pghost} "
    }

    // We write the grounding queries to this SQL file
    val sqlFile = File.createTempFile(s"grounding", ".sql")
    val writer = new PrintWriter(sqlFile)
    val assignIdFile = File.createTempFile(s"assignId", ".sh")
    val assignidWriter = new PrintWriter(assignIdFile)
    log.info(s"""Writing grounding queries to file="${sqlFile}" """)

    // If skip_learning and use the last weight table, copy it before removing it
    if (skipLearning && weightTable.isEmpty()) {
      writer.println(copyLastWeightsSQL)
    }

    writer.println(createWeightsSQL)
    writer.println(createTempWeightsSQL)

    writer.println(createVariablesHoldoutSQL)

    // check whether Greenplum is used
    var usingGreenplum = false
    issueQuery(checkGreenplumSQL) { rs => 
      usingGreenplum = rs.getBoolean(1) 
    }

    log.info("Using Greenplum = " + usingGreenplum.toString)

    // assign id
    if (usingGreenplum) {
      val createAssignIdPrefix = "psql " + dbnameStr + pguserStr + pgportStr + pghostStr + " -c " + "\"\"\""
      assignidWriter.println(createAssignIdPrefix + createAssignIdFunctionSQL + "\"\"\"")
    } else {
      writer.println(createSequencesSQL)
    }
    var idoffset : Long = 0

    // Ground all variables in the schema
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      if (usingGreenplum) {
        val assignIdSQL = "psql " + dbnameStr + pguserStr + pgportStr + pghostStr + " -c " + "\"\"\"" +
          s""" SELECT fast_seqassign('${relation}', ${idoffset});""" + "\"\"\""
        assignidWriter.println(assignIdSQL)
        val getOffset = s"SELECT count(*) FROM ${relation};"
        issueQuery(getOffset) { rs =>
          idoffset = idoffset + rs.getLong(1);
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
      
      // Create a view for the inference input query
      writer.println(s"""
        DROP VIEW IF EXISTS ${factorDesc.name}_query_user CASCADE;
        CREATE VIEW ${factorDesc.name}_query_user AS ${factorDesc.inputQuery};
        """)


      // Ground weights for each inference rule
      val weightValue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case x : KnownFactorWeightVector => x.value
        case _ => 0.0
      }
      // log.info(" x.value = " + weightValue.toString)

      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight] | factorDesc.weight.isInstanceOf[KnownFactorWeightVector]


      // log.info(" isFixed = " + isFixed.toString)

      //TODO : Should change variables for weight 
      // generateWeightCmd(inference rule name(conv_layer0),variables)
      val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables)

      if(isFixed || factorDesc.weight.vectorLength==0){
        writer.println(s"""
            INSERT INTO ${WeightsTable}(initial_value, is_fixed, description, weight_lenght)
            SELECT ${weightValue} AS wValue, ${isFixed} AS wIsFixed, ${weightCmd} AS wCmd, 
            ${factorDesc.weight.vectorLength} AS wLenght
            FROM ${factorDesc.name}_query_user GROUP BY wValue, wIsFixed, wCmd, wLenght;""")
      }
      else{
        log.info("factorDesc.weight.vectorLength = " + factorDesc.weight.vectorLength.toString)
        writer.println(s"""
          DROP TABLE IF EXISTS tempNum CASCADE;
          create table tempNum (num INT);""")
        var numArray="["+(-factorDesc.weight.vectorLength to -1).toList.mkString(",")+"]"
        writer.println(s"""insert into tempNum (num) select unnest(array${numArray});""")

        writer.println(createTempWeightsSQL)
        writer.println(s"""
          INSERT INTO ${TempWeightsTable}(initial_value, is_fixed, description, weight_lenght)
          SELECT ${weightValue} AS wValue, ${isFixed} AS wIsFixed, ${weightCmd} AS wCmd,
          ${factorDesc.weight.vectorLength} AS wLenght
          FROM ${factorDesc.name}_query_user GROUP BY wValue, wIsFixed, wCmd, wLenght;""")
       
        writer.println(s"""
          INSERT INTO ${WeightsTable}(initial_value, is_fixed, description, weight_lenght)
          SELECT initial_value, is_fixed, description::text || num::text, weight_lenght from tempNum,${TempWeightsTable};""")
      }
    }
    // we will have dd-graph-weights


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
    assignidWriter.close()

    log.info("Executing grounding query...")
    executeCmd(assignIdFile.getAbsolutePath())
    execute(Source.fromFile(sqlFile).getLines.mkString)
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