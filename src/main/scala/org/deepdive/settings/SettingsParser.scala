package org.deepdive.settings

import org.deepdive.Logging
import org.deepdive.Context
import org.deepdive.helpers.Helpers
import com.typesafe.config._
import scala.collection.JavaConversions._
import scala.util.Try

object SettingsParser extends Logging {

   def loadFromConfig(rootConfig: Config) : Settings = {
    val config = rootConfig.getConfig("deepdive")

    val inferenceSettings = loadInferenceSettings(config)
    val dbSettings = loadDbSettings(config)
    val schemaSettings = loadSchemaSettings(config)
    val extractors = loadExtractionSettings(config)
    val calibrationSettings = loadCalibrationSettings(config)
    val samplerSettings = loadSamplerSettings(config)
    val pipelineSettings = loadPipelineSettings(config)

    // Make sure that the variables related to the Greenplum distributed
    // filesystem are set if the user wants to use parallel grounding
    if (dbSettings.gpload) {
      if (dbSettings.gphost == "" || dbSettings.gpport == "" || dbSettings.gppath == "") {
        throw new RuntimeException(s"Parallel Loading is set to true, but one of db.default.gphost, db.default.gpport, or db.default.gppath is not specified")
      }
    }

    Settings(schemaSettings, extractors, inferenceSettings,
      calibrationSettings, samplerSettings, pipelineSettings, dbSettings)
  }

  // Returns: case class DbSetting(driver: String, url: String, user: String, password: String, dbname: String, host: String, port: String)
  private def loadDbSettings(config: Config) : DbSettings = {
    val dbConfig = Try(config.getConfig("db.default")).getOrElse {
      log.warning("No schema defined.")
      return DbSettings(Helpers.PsqlDriver, null, null, null, null, null, null,
        null, null, null, false, null)
    }
    val driver = Try(dbConfig.getString("driver")).getOrElse(null)
    val url = Try(dbConfig.getString("url")).getOrElse(null)
    val user = Try(dbConfig.getString("user")).getOrElse(null)
    val password = Try(dbConfig.getString("password")).getOrElse(null)
    val dbname = Try(dbConfig.getString("dbname")).getOrElse(null)
    val host = Try(dbConfig.getString("host")).getOrElse(null)
    val port = Try(dbConfig.getString("port")).getOrElse(null)
    val gphost = Try(dbConfig.getString("gphost")).getOrElse("")
    val gpport = Try(dbConfig.getString("gpport")).getOrElse("")
    var gppath = Try(dbConfig.getString("gppath")).getOrElse("")
    val parallelGrounding = Try(config.getConfig("inference").getBoolean("parallel_grounding")).getOrElse(false)
    var gpload = Try(dbConfig.getBoolean("gpload")).getOrElse(false) || parallelGrounding
    if (gppath.takeRight(1) == "/") gppath = gppath.take(gppath.length -1)
    log.info(s"Database settings: user ${user}, dbname ${dbname}, host ${host}, port ${port}.")
    if (gphost != "") {
      log.info(s"GPFDIST settings: host ${gphost} port ${gpport} path ${gppath}")
    }
    val incrementalModeStr = Try(dbConfig.getString("incremental_mode")).getOrElse("ORIGINAL")
    val incrementalMode = incrementalModeStr match {
      case "INCREMENTAL" => IncrementalMode.INCREMENTAL
      case "MATERIALIZATION" => IncrementalMode.MATERIALIZATION
      case _ => IncrementalMode.ORIGINAL
    }
    val schemaConfig = Try(config.getConfig("schema")).getOrElse {
      log.warning("No schema defined.")
      null
    }
    val keyConfig = Try(schemaConfig.getConfig("keys")).getOrElse(null)
    val keyMap = keyConfig match {
      case null => null
      case _ => {
        val keyRelations = keyConfig.root.keySet.toList
        val keyRelationsWithConfig = keyRelations.zip(keyRelations.map(keyConfig.getStringList))
        keyRelationsWithConfig.groupBy(_._1).map { case (k,v) => (k,v.map(_._2).flatten.distinct)}
      }
    }
    return DbSettings(driver, url, user, password, dbname, host, port,
      gphost, gppath, gpport, gpload, incrementalMode, keyMap)
  }


  private def loadSchemaSettings(config: Config) : SchemaSettings = {
    val schemaConfig = Try(config.getConfig("schema")).getOrElse {
      log.warning("No schema defined.")
      return SchemaSettings(Nil.toMap, None)
    }
    val variableConfig = schemaConfig.getConfig("variables")
    val relations = variableConfig.root.keySet.toList
    val relationsWithConfig = relations.zip(relations.map(variableConfig.getConfig))
    val variableMap = relationsWithConfig.flatMap { case(relation, relationConf) =>
      relationConf.root.keySet.map { attributeName =>
        val dataTypeStr = relationConf.getString(attributeName)
        val dataType = DataTypeParser.parse(DataTypeParser.dataType, dataTypeStr).getOrElse {
          throw new RuntimeException(s"Unknown data type: ${dataTypeStr}")
        }
        Tuple2(s"${relation}.${attributeName}", dataType)
      }
    }.toMap
    val setupFile = Try(schemaConfig.getString("setup"))
    SchemaSettings(variableMap, setupFile.toOption)
  }

  private def loadExtractionSettings(config: Config) : ExtractionSettings = {
    val extractionConfig = Try(config.getConfig("extraction")).getOrElse {
      return ExtractionSettings(Nil, 1)
    }
    val extractorParallelism = Try(extractionConfig.getInt("parallelism")).getOrElse(1)
    val extractors = extractionConfig.getObject("extractors").keySet().map { extractorName =>
      val extractorConfig = extractionConfig.getConfig(s"extractors.$extractorName")
      val style = Try(extractorConfig.getString(s"style")).getOrElse("json_extractor")
      style match {
        case "json_extractor" | "plpy_extractor" | "tsv_extractor" | "piggy_extractor" =>
          val outputRelation = extractorConfig.getString ("output_relation")
          val inputQuery = InputQueryParser.parse (InputQueryParser.inputQueryExpr,
            extractorConfig.getString (s"input") ).getOrElse {
            throw new RuntimeException (s"parsing ${
              extractorConfig.getString (s"input")
            } failed")
          }
          val udf = extractorConfig.getString (s"udf")
          val udfDir = Try (extractorConfig.getString (s"udf_dir") ).getOrElse(null)
          if (style == "piggy_extractor" && (udfDir == null || udf == null)) {
            throw new RuntimeException("you must specify udf_dir and udf for piggy extractors")
          }
          val sqlQuery = Try (extractorConfig.getString (s"sql") ).getOrElse ("")
          val cmd = Try (extractorConfig.getString ("cmd") ).toOption
          val parallelism = Try (extractorConfig.getInt (s"parallelism") ).getOrElse (1)
          val inputBatchSize = Try (extractorConfig.getInt (s"input_batch_size") ).getOrElse (10000)
          val outputBatchSize = Try (extractorConfig.getInt (s"output_batch_size") ).getOrElse (50000)
          val dependencies = Try (extractorConfig.getStringList ("dependencies").toSet).getOrElse (Set () )
          val beforeScript = Try (extractorConfig.getString ("before") ).toOption
          val afterScript = Try (extractorConfig.getString ("after") ).toOption
          val loader = Try (extractorConfig.getString ("loader") ).getOrElse("")
          val loaderConfigObj = Try (extractorConfig.getConfig ("loader_config") ).getOrElse(null)
          val loaderConfig = loaderConfigObj match {
            case null => null
            case _ => LoaderConfig (
                loaderConfigObj.getString("connection"),
                loaderConfigObj.getString("schema"),
                Try(loaderConfigObj.getInt("threads")).getOrElse(parallelism),
                Try(loaderConfigObj.getInt("parallel_transactions")).getOrElse(60)
            )
          }
          Extractor(extractorName, style, outputRelation, inputQuery, udfDir, udf, parallelism,
            inputBatchSize, outputBatchSize, dependencies, beforeScript, afterScript, sqlQuery, cmd,
            loader, loaderConfig)

        case "sql_extractor" | "cmd_extractor" =>
          val sqlQuery = Try (extractorConfig.getString (s"sql") ).getOrElse ("")
          val cmd = Try (extractorConfig.getString ("cmd") ).toOption
          val dependencies = Try (extractorConfig.getStringList ("dependencies").toSet).getOrElse (Set () )
          val beforeScript = Try (extractorConfig.getString ("before") ).toOption
          val afterScript = Try (extractorConfig.getString ("after") ).toOption
          Extractor(extractorName, style, "", null, null, "", 1,
            10000, 50000, dependencies, beforeScript, afterScript, sqlQuery, cmd)
      }
    }.toList
    ExtractionSettings(extractors, extractorParallelism)
  }

  private def loadInferenceSettings(config: Config): InferenceSettings = {
    val inferenceConfig = Try(config.getConfig("inference")).getOrElse {
      return InferenceSettings(Nil, None, false, "", false)
    }
    // These configs are currently disabled in grounding
    // // Zifei's changes: Add capability to skip learning
    val skipLearning = Try(inferenceConfig.getBoolean("skip_learning")).getOrElse(false)
    val weightTable = Try(inferenceConfig.getString("weight_table")).getOrElse("")
    val parallelGrounding = Try(inferenceConfig.getBoolean("parallel_grounding")).getOrElse(false)
    // val skipLearning = false
    // val weightTable = ""

    // if (!weightTable.isEmpty() && skipLearning == false) {
    //   log.error("inference.skip_learning must be true when inference.weight_table is assigned!")
    //   throw new RuntimeException(s"skip_learning assertion failed")
    // }

    val batchSize = Try(inferenceConfig.getInt("batch_size")).toOption
    val factorConfig = Try(inferenceConfig.getObject("factors")).getOrElse {
      return InferenceSettings(Nil, batchSize, skipLearning, weightTable, parallelGrounding)
    }
    val factors = factorConfig.keySet().map { factorName =>
      val factorConfig = inferenceConfig.getConfig(s"factors.$factorName")
      val factorInputQuery = InputQueryParser.parse(InputQueryParser.DatastoreInputQueryExpr,
        factorConfig.getString("input_query")).getOrElse {
        throw new RuntimeException(s"parsing ${factorConfig.getString("input_query")} failed")
      }.query
      val factorFunction = FactorFunctionParser.parse(
        FactorFunctionParser.factorFunc, factorConfig.getString("function")).getOrElse {
        throw new RuntimeException(s"parsing ${factorConfig.getString("function")} failed")
      }
      val factorWeight = FactorWeightParser.parse(
        FactorWeightParser.factorWeight, factorConfig.getString("weight")).getOrElse {
        throw new RuntimeException(s"parsing ${factorConfig.getString("weight")} failed")
      }
      val factorWeightPrefix = Try(factorConfig.getString("weightPrefix")).getOrElse(factorName)
      FactorDesc(factorName, factorInputQuery, factorFunction,
          factorWeight, factorWeightPrefix)
    }.toList
    InferenceSettings(factors, batchSize, skipLearning, weightTable, parallelGrounding)
  }

  private def loadCalibrationSettings(config: Config) : CalibrationSettings = {
    val calibrationConfig = Try(config.getConfig("calibration")).getOrElse {
      return CalibrationSettings(0.0, None, None)
    }
    val holdoutFraction = Try(calibrationConfig.getDouble("holdout_fraction")).getOrElse(0.0)
    val holdoutQuery = Try(calibrationConfig.getString("holdout_query")).toOption
    val observationQuery = Try(calibrationConfig.getString("observation_query")).toOption

    CalibrationSettings(holdoutFraction, holdoutQuery, observationQuery)
  }

  private def loadSamplerSettings(config: Config) : SamplerSettings = {
    val samplingConfig = config.getConfig("sampler")
    val samplerCmd = samplingConfig.getString("sampler_cmd")

    // Zifei's changes: Add capability to skip learning
    // If skip learning, set "-l 0" in sampler
    val skipLearning = Try(config.getConfig("inference").getBoolean("skip_learning")).getOrElse(false)
    val samplerArgs = skipLearning match {
      case false =>
        samplingConfig.getString("sampler_args")
      case true =>
        samplingConfig.getString("sampler_args").replaceAll("""-l +[0-9]+ *""", """-l 0 """)
    }
    log.debug(s"samplerArgs: ${samplerArgs}")

    SamplerSettings(samplerCmd, samplerArgs)

  }

  private def loadPipelineSettings(config: Config) : PipelineSettings = {
    val pipelineConfig = Try(config.getConfig("pipeline")).getOrElse {
      return PipelineSettings(None, Nil, null, None)
    }
    val relearnFrom = Try(pipelineConfig.getString("relearn_from")).getOrElse(null)
    val activePipeline = Try(pipelineConfig.getString("run")).toOption
    val baseDir = Try(pipelineConfig.getString("base_dir")).toOption
    if (relearnFrom == null) {
      val pipelinesObj = Try(pipelineConfig.getObject("pipelines")).getOrElse {
        return PipelineSettings(None, Nil, null, None)
      }
      val pipelines = pipelinesObj.keySet().map { pipelineName =>
        val tasks = pipelineConfig.getStringList(s"pipelines.$pipelineName").toSet
        Pipeline(pipelineName, tasks)
      }.toList
      return PipelineSettings(activePipeline, pipelines, relearnFrom, baseDir)
    }
    PipelineSettings(activePipeline, Nil, relearnFrom, baseDir)
  }
}




import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object DataTypeParser extends RegexParsers {
  def CategoricalParser = "Categorical" ~> "(" ~> """\d+""".r <~ ")" ^^ { n => MultinomialType(n.toInt) }
  def BooleanParser = "Boolean" ^^ { s => BooleanType }
  def dataType = CategoricalParser | BooleanParser
}

import org.deepdive.Logging
import scala.util.parsing.combinator.RegexParsers
import scala.language.postfixOps

object FactorFunctionParser extends RegexParsers with Logging {
  def relationOrField = """[\w]+""".r
  def arrayDefinition = """\[\]""".r
  def equalPredicate = """[0-9]+""".r

  def implyFactorFunction = ("Imply" | "IMPLY") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    ImplyFactorFunction(varList)
  }

  def orFactorFunction = ("Or" | "OR") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    OrFactorFunction(varList)
  }

  def xorFactorFunction = ("Xor" | "XOR") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    XorFactorFunction(varList)
  }

  def andFactorFunction = ("And" | "AND") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    AndFactorFunction(varList)
  }

  def equalFactorFunction = ("Equal" | "EQUAL") ~> "(" ~> factorVariable ~ ("," ~> factorVariable) <~ ")" ^^ {
    case v1 ~ v2 =>
    EqualFactorFunction(List(v1, v2))
  }

  def isTrueFactorFunction = ("IsTrue" | "ISTRUE") ~> "(" ~> factorVariable <~ ")" ^^ { variable =>
    IsTrueFactorFunction(List(variable))
  }

  def multinomialFactorFunction = ("Multinomial" | "MULTINOMIAL") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    MultinomialFactorFunction(varList)
  }

  def linearFactorFunction = ("Linear" | "LINEAR") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    LinearFactorFunction(varList)
  }

  def ratioFactorFunction = ("Ratio" | "RATIO") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    RatioFactorFunction(varList)
  }

  def logicalFactorFunction = ("Logical" | "LOGICAL") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    LogicalFactorFunction(varList)
  }

  def factorVariable = ("!"?) ~ rep1sep(relationOrField, ".") ~ (arrayDefinition?) ~
    (("=" ~> equalPredicate)?) ^^ {
    case (isNegated ~ varList ~ isArray ~ predicate)  =>
      FactorFunctionVariable(varList.take(varList.size - 1).mkString("."), varList.last,
        isArray.isDefined, isNegated.isDefined, readLong(predicate))
  }

  def readLong(predicate: Option[String]) : Option[Long] = {
    predicate match {
      case Some(number) => Some(number.toLong)
      case None => None
    }
  }

  def factorFunc = implyFactorFunction | orFactorFunction | andFactorFunction |
    equalFactorFunction | isTrueFactorFunction | xorFactorFunction | multinomialFactorFunction |
    linearFactorFunction | ratioFactorFunction | logicalFactorFunction

}

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object FactorWeightParser extends RegexParsers {
  def relationOrField = """[^,()]+""".r
  def weightVariable = relationOrField

  def constantWeight = """-?[\d\.]+""".r ^^ { x => KnownFactorWeight(x.toDouble) }
  def unknownWeight = "?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }
  def factorWeight = constantWeight | unknownWeight

}

import scala.util.parsing.combinator.RegexParsers

object InputQueryParser extends RegexParsers {

  def filenameExpr =  "'" ~> """[^']+""".r <~ "'"
  def CSVInputQueryExpr = "CSV" ~> "(" ~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, ',') }
  def TSVInputQueryExpr = "TSV" ~> "("~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, '\t') }
  def DatastoreInputQueryExpr = not("CSV") ~> not("TSV") ~> "[\\w\\W]+".r ^^ { str =>
    val withoutColon = """;\s+\n?$""".r.replaceAllIn(str, "")
    val result = """[\s\n]+""".r replaceAllIn(withoutColon, " ")
    DatastoreInputQuery(result)
  }
  def inputQueryExpr = (CSVInputQueryExpr | TSVInputQueryExpr | DatastoreInputQueryExpr)

}

