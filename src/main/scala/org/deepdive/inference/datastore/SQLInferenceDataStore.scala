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
import org.deepdive.Context
// import scala.collection.mutable.Map


/* Stores the factor graph and inference results. */
trait SQLInferenceDataStoreComponent extends InferenceDataStoreComponent {

  def inferenceDataStore : SQLInferenceDataStore

}

trait SQLInferenceDataStore extends InferenceDataStore with Logging {

  val hostname = "madmax6"

  val port = 8083

  def ds : JdbcDataStore

  val factorOffset = new java.util.concurrent.atomic.AtomicLong(0)

  /* Internal Table names */
  def WeightsTable = "dd_graph_weights"
  // def lastWeightsTable = "dd_graph_last_weights"
  def FactorsTable = "dd_graph_factors"
  def VariablesTable = "dd_graph_variables"
  def VariablesMapTable = "dd_graph_variables_map"
  // def EdgesTable = "dd_graph_edges"
  def WeightResultTable = "dd_inference_result_weights"
  def VariableResultTable = "dd_inference_result_variables"
  def MappedInferenceResultView = "dd_mapped_inference_result"
  def IdSequence = "id_sequence"
  // def EdgesCountTable = "dd_graph_edges_count"

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
        // TODO: Call TaskManager to kill all tasks
    }
  }


  /* Issues a query */
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

    /* Issues a query */
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

  /* Issues a query */
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {

    val conn = ds.borrowConnection()
    conn.setAutoCommit(false);
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(10000);
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
      id bigint, 
      weight double precision);
  """

  def createInferenceResultIndiciesSQL = s"""
    DROP INDEX IF EXISTS ${WeightResultTable}_idx CASCADE;
    DROP INDEX IF EXISTS ${VariableResultTable}_idx CASCADE;
    CREATE INDEX ${WeightResultTable}_idx ON ${WeightResultTable} (weight);
    CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} (expectation);
  """

  def createInferenceViewSQL(relationName: String, columnName: String) = s"""
    DROP VIEW IF EXISTS ${relationName}_${columnName}_inference CASCADE;
    CREATE VIEW ${relationName}_${columnName}_inference AS
    (SELECT ${relationName}.*, mir.category, mir.expectation FROM
    ${relationName}, ${VariableResultTable} mir
    WHERE ${relationName}.id = mir.id);
  """

  /*
  def createInferenceViewSQL(relationName: String, columnName: String) = s"""
    CREATE VIEW ${relationName}_${columnName}_inference AS
    (SELECT ${relationName}.*, mir.category, mir.expectation FROM
    ${relationName}, ${VariableResultTable} mir
    WHERE ${relationName}.id = mir.id
    ORDER BY mir.expectation DESC);
  """
  */

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

  def init() : Unit = {
  }

  val weightMap = scala.collection.mutable.Map[String, Long]()

  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    factorDescs: Seq[FactorDesc], holdoutFraction: Double,
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String) : Unit = {

  }

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String) {

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
      }
      
      executeQuery(s"""
        DROP EXTERNAL TABLE IF EXISTS variables_${relation} CASCADE;
        CREATE WRITABLE EXTERNAL TABLE variables_${relation} (
          id bigint,
          num_rows bigint,
          num_cols bigint,
          is_evidence int[],
          initial_value double precision) 
        LOCATION ('gpfdist://${hostname}:${port}/variables_${relation}')
        FORMAT 'TEXT';
        """)

      executeQuery(s"""
        INSERT INTO variables_${relation}(id, num_rows, num_cols, is_evidence, initial_value)
        (SELECT id, width, length, (${column} IS NOT NULL)::int, COALESCE(${column}::int,0) 
          FROM ${relation})
        """)

    }
    

    executeQuery(s"""DROP EXTERNAL TABLE IF EXISTS factormeta CASCADE;""")
    executeQuery(s"""CREATE WRITABLE EXTERNAL TABLE factormeta (name text, funcid text, sign text)
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
    	 executeQuery(s"""INSERT INTO factormeta VALUES ('${factorDesc.name}', '${functionName}', '${s}');""")
    }
    
    executeQuery(s"""DROP EXTERNAL TABLE IF EXISTS weights CASCADE;""")
    executeQuery(s"""CREATE WRITABLE EXTERNAL TABLE weights (id bigint, num_rows int, num_cols int, isfixed int[], initvalue float[]) 
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
      executeQuery(s"""DROP TABLE IF EXISTS edges CASCADE;""")

      executeQuery(s"""CREATE TABLE queryview_${factorDesc.name} AS ${factorDesc.inputQuery};""")

      val weightlist = factorDesc.weight.variables.map ( v => s"""${v}""" ).mkString(" , ")
      var isfixed = 0
      if(factorDesc.weight.isInstanceOf[KnownFactorWeight]){
        isfixed = 1
      }
      val initvalue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case x : KnownFactorWeightVector => x.value
        case _ => 0.0
      }
      var weightLength = Math.sqrt(factorDesc.weight.vectorLength.toDouble)
      val functionName = factorDesc.func.getClass.getSimpleName

      if(factorDesc.weight.isInstanceOf[KnownFactorWeight] == true || weightlist == ""){
        executeQuery(s"""INSERT INTO weights VALUES (${cweightid}, ${weightLength}, ${weightLength}, ${isfixed}, ${initvalue});""")
        executeQuery(s"""CREATE TABLE edges AS
                    SELECT SELECT t0.ids AS in_ids, t0.locations AS in_locations, t0.id AS out_id, t0.location AS out_location,
                      t0.num_ids AS num_ids, ${functionName}, ${cweightid}::bigint
                    FROM queryview_${factorDesc.name};
                """)
        cweightid = cweightid + 1
      }else{
        var isfixed = 0
        val initvalue = 0
        val weightjoinlist = factorDesc.weight.variables.map ( v => s"""t0.${v}=t1.${v}""" ).mkString(" AND ")
        executeQuery(s"""CREATE TABLE weighttable_${factorDesc.name} AS
                    SELECT ${weightlist}, 0::bigint id, ${weightLength}:int num_rows, ${weightLength}:int num_cols, ${isfixed}::int isfixed, ${initvalue}::float initvalue 
                    FROM queryview_${factorDesc.name} 
                    GROUP BY ${weightlist}
                    DISTRIBUTED BY (${weightlist});""")
        executeQuery(s"""SELECT fast_seqassign('weighttable_${factorDesc.name}', ${cweightid});""")
        executeQuery(s"""CREATE TABLE edges AS
                    SELECT t0.ids AS in_ids, t0.locations AS in_locations, t0.id AS out_id, t0.location AS out_location,
                      t0.num_ids AS num_ids, ${functionName}, t1.id
                    FROM queryview_${factorDesc.name} t0, weighttable_${factorDesc.name} t1
                    WHERE ${weightjoinlist};
                """)

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

    //execute(createInferenceResultSQL)
    //execute(createInferenceResultWeightsSQL)
    // execute(createMappedInferenceResultView)

    log.info("Copying inference result weights...")
    bulkCopyWeights(weightsOutputFile)
    log.info("Copying inference result variables...")
    //bulkCopyVariables(variableOutputFile)
    log.info("Creating indicies on the inference result...")
    //execute(createInferenceResultIndiciesSQL)

    // Each (relation, column) tuple is a variable in the plate model.
     // Find all (relation, column) combinations
    val relationsColumns = variableSchema.keys map (_.split('.')) map {
      case Array(relation, column) => (relation, column)
    }

    // execute(createMappedWeightsViewSQL)

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
