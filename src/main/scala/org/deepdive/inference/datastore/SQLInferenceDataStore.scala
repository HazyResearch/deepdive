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
  def FactorsTable = "dd_graph_factors"
  def VariablesTable = "dd_graph_variables"
  def VariablesMapTable = "dd_graph_variables_map"
  def WeightResultTable = "dd_inference_result_weights"
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
        // TODO: Call TaskManager to kill all tasks
    }
  }

  /* Issues a query */
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {

    val conn = ds.borrowConnection()
    conn.setAutoCommit(false);
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(1000);
    val rs = stmt.executeQuery(sql)
    while(rs.next()){
      op(rs)
    }
    conn.close()
  }

  /* get database product name */
  def getDBname = {
    val conn = ds.borrowConnection()
    val metadata = conn.getMetaData()
    metadata.getDatabaseProductName()
  }

  def selectAsMap(sql: String) : List[Map[String, Any]] = {
    ds.DB.readOnly { implicit session =>
      SQL(sql).map(_.toMap).list.apply()
    }
  }

  def keyType = "bigserial"
  def stringType = "text"
  def randomFunc = "RANDOM()"

  def createWeightsSQL = s"""
    DROP TABLE IF EXISTS ${WeightsTable} CASCADE;
    CREATE TABLE ${WeightsTable}(
      id bigserial primary key,
      initial_value double precision,
      is_fixed boolean,
      description ${stringType});
    ALTER SEQUENCE ${WeightsTable}_id_seq MINVALUE -1 RESTART WITH 0;
  """

  def createSequencesSQL = s"""
    DROP SEQUENCE IF EXISTS ${IdSequence};
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
    SELECT id AS "id", is_fixed AS "is_fixed", initial_value AS "initial_value"
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

  def init() : Unit = {
  }

  def generateWeightCmd(weightPrefix: String, weightVariables: Seq[String]) : String = 
    weightVariables.map ( v => s"""(CASE WHEN "${v}" IS NULL THEN '' ELSE "${v}"::text END)""" )
      .mkString(" || ") match {
      case "" => s"""'${weightPrefix}-' """
      case x => s"""'${weightPrefix}-' || ${x}"""
  }

  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    factorDescs: Seq[FactorDesc], holdoutFraction: Double,
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String) : Unit = {

    var numVariables  : Long = 0
    var numFactors    : Long = 0
    var numWeights    : Long = 0
    var numEdges      : Long = 0

    val randomGen = new Random()

    
    log.info(s"Dumping factor graph...")
    log.info("Dumping variables...")
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      
      val selectVariablesForDumpSQL = s"""
        SELECT id, (${variable} IS NOT NULL), ${variable}::int
        FROM ${relation}"""

      val cardinality = dataType match {
        case BooleanType => 0
        case MultinomialType(x) => x.toLong
      }

      issueQuery(selectVariablesForDumpSQL) { rs =>
        var isEvidence = rs.getBoolean(2)
        if (isEvidence && randomGen.nextFloat < holdoutFraction) {
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
      serializer.addWeight(rs.getLong(1), rs.getBoolean(2), 
        rs.getDouble(3))
      numWeights += 1
    }


    log.info("Dumping factors...")
    factorDescs.foreach { factorDesc =>
      val functionName = factorDesc.func.getClass.getSimpleName
      val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables)

      val selectInputQueryForDumpSQL = s"""
        SELECT ${factorDesc.name}_query_user.*, ${WeightsTable}.id AS "wid"
        FROM ${factorDesc.name}_query_user, ${WeightsTable}
        WHERE ${weightCmd} = ${WeightsTable}.description"""

      val variables = factorDesc.func.variables

      issueQuery(selectInputQueryForDumpSQL) { rs =>

        serializer.addFactor(numFactors, rs.getLong("wid"), functionName, variables.length)

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

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String) {

    // We write the grounding queries to this SQL file
    val sqlFile = File.createTempFile(s"grounding", ".sql")
    val writer = new PrintWriter(sqlFile)
    log.info(s"""Writing grounding queries to file="${sqlFile}" """)

    // if (skipLearning == true && weightTable.isEmpty()) {
    //   writer.println(copyLastWeightsSQL)
    // }

    writer.println(createSequencesSQL)
    writer.println(createWeightsSQL)

    // Ground all variables in the schema
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')

      // TODO: this is expensive
      writer.println(s"""
        UPDATE ${relation} SET id = nextval('${IdSequence}');
        """)

    }

    // Create table for each inference rule
    factorDescs.foreach { factorDesc =>
      
      // input query
      writer.println(s"""
        DROP VIEW IF EXISTS ${factorDesc.name}_query_user CASCADE;
        CREATE VIEW ${factorDesc.name}_query_user AS (${factorDesc.inputQuery});
        """)


      // Ground weights for each inference rule
      val weightValue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }

      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables)

      writer.println(s"""
        INSERT INTO ${WeightsTable}(initial_value, is_fixed, description)
        SELECT ${weightValue} AS wValue, ${isFixed} AS wIsFixed, ${weightCmd} AS wCmd
        FROM ${factorDesc.name}_query_user GROUP BY wValue, wIsFixed, wCmd;""")
    }

    writer.println(s"""CREATE INDEX ${WeightsTable}_desc_idx ON ${WeightsTable}(description);""")
    writer.println(s"""ANALYZE ${WeightsTable};""")

    writer.close()

    log.info("Executing grounding query...")
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
