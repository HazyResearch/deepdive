package org.deepdive.inference

import org.deepdive.calibration.{BucketData, Bucket}
import org.deepdive.helpers.Helpers
import org.deepdive.{Context, Logging}
import org.deepdive.datastore.{DataLoader, JdbcDataStore}
import org.deepdive.settings._
import scala.sys.process._
import scala.sys.process.ProcessLogger

trait AbstractInferenceRunner extends InferenceRunner with Logging {

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
  def FeatureStatsSupportTable = "dd_feature_statistics_support"
  def FeatureStatsView = "dd_feature_statistics"

  def init() : Unit = {}

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

    // assign variable id - sequential and unique
    assignVariablesIds(schema)

    // assign holdout
    assignHoldout(schema, calibrationSettings)

    // generate cardinality tables
    generateCardinalityTables(schema)

    // ground variables
    groundVariables(schema, du, dbSettings, groundingPath)

    // generate factor meta data
    groundFactorMeta(du, factorDescs, dbSettings, groundingPath)

    // ground weights and factors
    groundFactorsAndWeights(factorDescs, calibrationSettings, du, dbSettings,
      groundingPath, skipLearning, weightTable)

    // create inference result table
    createInferenceResult

    createInferenceResultWeights

    // split grounding files and transform to binary format
    convertGroundingFormat(groundingPath)
  }

  def assignVariablesIds(schema: Map[String, _ <: VariableDataType])

  def assignHoldout(schema: Map[String, _ <: VariableDataType], calibrationSettings: CalibrationSettings)

  def generateCardinalityTables(schema: Map[String, _ <: VariableDataType])

  def groundVariables(schema: Map[String, _ <: VariableDataType], du: DataLoader,
                      dbSettings: DbSettings, groundingPath: String)

  // convert grounding file format to be compatible with sampler
  // for more information about format, please refer to deepdive's website
  def convertGroundingFormat(groundingPath: String) {
    log.info("Converting grounding file format...")
    // TODO: this python script is dangerous and ugly. It changes too many states!
    val cmd = s"python ${InferenceNamespace.getFormatConvertingScriptPath} ${groundingPath} " +
      s"${InferenceNamespace.getFormatConvertingWorkerPath} ${Context.outputDir}"
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

  def groundFactorMeta(du: DataLoader, factorDescs: Seq[FactorDesc], dbSettings: DbSettings,
                       groundingPath: String)


  ///////////////////////////////// groundFactorsAndWeights

  def groundFactorsAndWeights(factorDescs: Seq[FactorDesc],
                              calibrationSettings: CalibrationSettings, du: DataLoader,
                              dbSettings: DbSettings, groundingPath: String,
                              skipLearning: Boolean, weightTable: String) {

    // save last weights
    if (skipLearning && weightTable.isEmpty()) {
      copyLastWeights
    }

    createWeightsTable

    createFeatureStatsSupportTable

    // weight and factor id
    // greenplum: use fast_seqassign postgres: use sequence
    var cweightid : Long = 0
    var factorid : Long = 0
    val weightidSequence = "dd_weight_sequence"
    val factoridSequence = "dd_factor_sequence"
    createSequenceFunction(weightidSequence)
    createSequenceFunction(factoridSequence)

    factorDescs.zipWithIndex.foreach { case (factorDesc, idx) =>

      factorid += createFactorQueryTableWithId(factorDesc, factorid, factoridSequence)

      factorDesc.func match {
        case ImplyFactorFunction(_) | OrFactorFunction(_) | AndFactorFunction(_) | EqualFactorFunction(_) |
             IsTrueFactorFunction(_) | XorFactorFunction(_) =>
          cweightid += createBooleanFactorWeightTableWithId(factorDesc, cweightid, weightidSequence,
            du, groundingPath, dbSettings)
        case MultinomialFactorFunction(_) =>
          cweightid += createMultinomialFactorWeightTableWithId(factorDesc, cweightid, weightidSequence,
            du, groundingPath, dbSettings)
      }

      // create feature stats for boolean LR
      createFeatureStats(factorDesc)
    }

    if (skipLearning) {
      reuseWeights(weightTable)
    }

    // dump weights
    dumpWeights(du, groundingPath, dbSettings)
  }

  def copyLastWeights

  def createWeightsTable

  def createFeatureStatsSupportTable

  def createSequenceFunction(name:String)

  def createFactorQueryTableWithId(factorDesc:FactorDesc, startId:Long, sequenceName:String): Long

  def createBooleanFactorWeightTableWithId(factorDesc:FactorDesc, cweightid:Long, weightidSequence:String,
                                           du:DataLoader, groundingPath:String, dbSettings: DbSettings):Long

  def createMultinomialFactorWeightTableWithId(factorDesc:FactorDesc, cweightid:Long, weightidSequence:String,
                                               du:DataLoader, groundingPath:String, dbSettings: DbSettings):Long

  def createFeatureStats(factorDesc: FactorDesc)

  def reuseWeights(weightTable: String)

  def dumpWeights(du:DataLoader, groundingPath:String, dbSettings: DbSettings)



  //////////////////////////// CALIBRATION

  def getCalibrationData(variable: String, dataType: VariableDataType,
                         buckets: List[Bucket]) : Map[Bucket, BucketData] = {

    val Array(relationName, columnName) = variable.split('.')
    val inferenceViewName = s"${relationName}_${columnName}_inference"
    val bucketedViewName = s"${relationName}_${columnName}_inference_bucketed"
    val calibrationViewName = s"${relationName}_${columnName}_calibration"

    createBucketedCalibrationView(bucketedViewName, inferenceViewName, buckets)
    log.info(s"created calibration_view=${calibrationViewName}")
    dataType match {
      case BooleanType =>
        createCalibrationViewBoolean(calibrationViewName, bucketedViewName, columnName)
      case MultinomialType(_) =>
        createCalibrationViewMultinomial(calibrationViewName, bucketedViewName, columnName)
    }

    val bucketData = selectCalibrationData(calibrationViewName).map { row =>
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

  def createCalibrationViewBoolean(name: String, bucketedView: String, columnName: String)

  def createCalibrationViewMultinomial(name: String, bucketedView: String, columnName: String)

  def createBucketedCalibrationView(name: String, inferenceViewName: String, buckets: List[Bucket])

  def selectCalibrationData(name: String):List[Map[String, Any]]

  //////////////////////////// writebackInferenceResult

  /**
   * This function is executed when sampler finished.
   */
  def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
                               variableOutputFile: String, weightsOutputFile: String, dbSettings: DbSettings) = {

    createInferenceResult
    createInferenceResultWeights

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
    createMappedWeightsView

    // Create feature statistics tables for error analysis
    createMappedFeatureStatsView

    relationsColumns.foreach { case(relationName, columnName) =>
      createInferenceView(relationName, columnName)
    }
  }

  def createInferenceResult

  def createInferenceResultWeights

  def createInferenceView(relationName:String, columnName:String)

  /**
   * weightsFile: binary format. Assume "weightsFile" file exists.
   */
  def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit

  /**
   * variablesFile: binary format. Assume "variablesFile" file exists.
   */
  def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit

  def createMappedWeightsView

  def createMappedFeatureStatsView

  //////////////////////////////// UTIL

  // given a path, get file/folder name
  // e.g., /some/path/to/folder -> folder
  def getFileNameFromPath(path: String) : String = {
    return new java.io.File(path).getName()
  }

}
