package org.deepdive.settings

import com.typesafe.config._
import org.deepdive.{Context, Logging}

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

object SettingsParser extends Logging {

  val loadFromConfig =
    getDeepDiveConfig _ andThen
      loadDatabaseSettings andThen
      loadSchemaSettings andThen
      loadExtractionSettings andThen
      loadInferenceSettings andThen
      loadSamplerSettings andThen
      loadCalibrationSettings andThen
      loadPipelineSettings andThen
      checkSettings

  def configEntries(config: Config): Iterable[(String, Config)] = {
    config.root.keys map { key => key -> config.getConfig(key) }
  }

  private def getDeepDiveConfig(rootConfig: Config): Settings = {
    Settings(config = (rootConfig withFallback ConfigFactory.load()) getConfig("deepdive"))
  }

  private def checkSettings(settings: Settings): Settings = {
    // val dbDriver = config.getString("deepdive.db.default.driver")
    val dbSettings = settings.dbSettings
    dbSettings.dbname match {
      case "" =>
        sys.error(s"parsing dbname failed")
      case _ =>
    }

    // check incremental settings
    dbSettings.incrementalMode match {
      case IncrementalMode.MATERIALIZATION | IncrementalMode.INCREMENTAL =>
        if (!settings.pipelineSettings.baseDir.isDefined) {
          log.error(s"base folder not set for incremental run")
          Context.shutdown()
        }
      case _ =>
    }

    // Make sure the activePipelineName is defined
    if (settings.pipelineSettings.activePipelineName.isDefined && settings.pipelineSettings.activePipeline.isEmpty) {
      sys.error(s"${settings.pipelineSettings.activePipelineName.get}: No such pipeline defined")
    }

    log.info(settings.config.root.render(ConfigRenderOptions.concise().setFormatted(true)))
    log.info(settings toString)

    settings
  }

  private def loadDatabaseSettings(settings: Settings): Settings = {
    var config = settings.config withFallback ConfigFactory.parseString(
      """
        |db.default {
        |  driver   : ""
        |  url      : ""
        |  user     : ""
        |  password : ""
        |  dbname   : ""
        |  host     : ""
        |  port     : ""
        |  gphost: ""
        |  gpport: ""
        |  gppath: ""
        |  gpload: false
        |  incremental_mode: ORIGINAL
        |}
        |
        |schema {
        |  keys {
        |  }
        |}
        |
        |inference {
        |  parallel_grounding: false
        |}
      """.stripMargin)
    var dbConfig = config.getConfig("db.default")
    // strip trailing slash
    val gppath: String = dbConfig.getString("gppath")
    if (gppath endsWith "/")
      dbConfig = dbConfig.withValue("gppath", ConfigValueFactory.fromAnyRef(
        gppath stripSuffix ("/")))

    // Make sure that the variables related to the Greenplum distributed
    // filesystem are set if the user wants to use parallel grounding
    if (dbConfig.getBoolean("gpload") || config.getBoolean("inference.parallel_grounding")) {
      if (dbConfig.getString("gphost").isEmpty || dbConfig.getString("gpport").isEmpty || gppath.isEmpty)
        sys.error(s"Parallel Loading is set to true, but one of db.default.gphost, db.default.gpport, or db.default.gppath is not specified")
    }
    log.info(s"Database settings: user ${dbConfig.getString("user")}, dbname ${dbConfig.getString("dbname")}, host ${dbConfig.getString("host")}, port ${dbConfig.getString("port")}.")
    if (dbConfig.getString("gphost") != "") {
      log.info(s"GPFDIST settings: host ${dbConfig.getString("gphost")} port ${dbConfig.getString("gpport")} path ${gppath}")
    }

    settings updatedConfig("db.default", dbConfig) copy(
      dbSettings = DbSettings(
        // TODO determine everything from DeepDive app's db.url
        driver = dbConfig.getString("driver"),
        url = dbConfig.getString("url"),
        user = dbConfig.getString("user"),
        password = dbConfig.getString("password"),
        dbname = dbConfig.getString("dbname"),
        host = dbConfig.getString("host"),
        port = dbConfig.getString("port"),
        gphost = dbConfig.getString("gphost"),
        gppath = gppath,
        gpport = dbConfig.getString("gpport"),
        gpload = dbConfig.getBoolean("gpload") || config.getBoolean("inference.parallel_grounding"),
        incrementalMode = dbConfig.getString("incremental_mode") match {
          case "INCREMENTAL" => IncrementalMode.INCREMENTAL
          case "MATERIALIZATION" => IncrementalMode.MATERIALIZATION
          case "ORIGINAL" => IncrementalMode.ORIGINAL
          case "MERGE" => IncrementalMode.ORIGINAL // XXX why does ddlog generate a MERGE mode?
          case incremental_mode => sys.error(s"${incremental_mode}: Invalid incremental_mode for db.default")
        },
        keyMap = config.getConfig("schema.keys") match {
          case keyConfig => keyConfig.root.keys map {
            case table => table -> keyConfig.getStringList(table).distinct.toList
          } toMap
        })
    )
  }

  private def loadSchemaSettings(settings: Settings): Settings = {
    val config = settings.config withFallback ConfigFactory.parseString(
      """
        |schema {
        |  keys {
        |  }
        |  variables {
        |  }
        |  setup: null
        |  images {
        |  }
        |}
      """.stripMargin)
    val variableConfig = config.getConfig("schema.variables")
    val fileConfig = config.getConfig("schema.images")
    settings updatedConfig(config) copy(
      schemaSettings = SchemaSettings(
        // This is complicated because we encode variable schema by abusing HOCON syntax,
        // e.g.: schema.variable { foo.bar: Boolean, table.column: Boolean }
        // which is equivalent to schema.variable { foo { bar: Boolean }, table { column: Boolean } }
        variables = configEntries(variableConfig) flatMap {
          case (relationName, relationConf) => // table names
            relationConf.root.keys map { attributeName => // attribute maps to table
              val variableType = DataTypeParser.parseVariableType(relationConf.getString(attributeName))
              s"${relationName}.${attributeName}" -> variableType
            }
        } toMap,
        setupFile = Try(config.getString("schema.setup")) toOption,
        images = configEntries(fileConfig) flatMap {
          case (relationName, relationConf) => // table names
            relationConf.root.keys map { attributeName => // attribute maps to table
              s"${relationName}.${attributeName}" -> relationConf.getString(attributeName)
            }
        } toMap
      )
    )
  }


  private def loadExtractionSettings(settings: Settings): Settings = {
    var config = settings.config withFallback ConfigFactory.parseString(
      """
        |extraction {
        |  parallelism: 1
        |  extractors {
        |  }
        |}
      """.stripMargin)
    val extractorConfigDefaultBase =
      """
        |output_relation: null
        |input: null
        |udf: null
        |parallelism: 1
        |input_batch_size: 10000
        |output_batch_size: 50000
        |dependencies: []
        |before: null
        |after: null
        |loader: ""
      """.stripMargin

    val extractorConfigDefaults = Map(
      "json_extractor" -> (extractorConfigDefaultBase),
      "tsv_extractor" -> (extractorConfigDefaultBase),
      "plpy_extractor" -> (extractorConfigDefaultBase),
      "piggy_extractor" -> (extractorConfigDefaultBase +
        """
          |udf_dir: null
        """.stripMargin),
      "cmd_extractor" -> (extractorConfigDefaultBase +
        """
          |cmd: null
        """.stripMargin),
      "sql_extractor" -> (extractorConfigDefaultBase +
        """
          |sql: null
        """.stripMargin)
    )
    val extractors = configEntries(config.getConfig("extraction.extractors")) map {
      case (extractorName, extractorConfigRaw) =>
      val style = Try(extractorConfigRaw.getString("style")).getOrElse("json_extractor")
      val extractorConfig = extractorConfigRaw withFallback
          ConfigFactory.parseString(extractorConfigDefaults(style))
      config = config.withValue(s"extraction.extractors.${extractorName}", extractorConfig.root)
      val extractor = Extractor(
        name = extractorName,
        style = style,
        parallelism = extractorConfig.getInt("parallelism"),
        inputBatchSize = extractorConfig.getInt("input_batch_size"),
        outputBatchSize = extractorConfig.getInt("output_batch_size"),
        dependencies = extractorConfig.getStringList("dependencies") toSet,
        beforeScript = Try(extractorConfig.getString("before")) toOption,
        afterScript = Try(extractorConfig.getString("after")) toOption
      )
      style match {
        case "json_extractor" |
             "plpy_extractor" |
             "tsv_extractor" |
             "piggy_extractor" =>
          val udfDir = Try(extractorConfig.getString("udf_dir")) toOption
          val udf = extractorConfig.getString("udf")
          style match {
            case "piggy_extractor" if ((udfDir isEmpty) || udf == null) =>
              sys.error("you must specify udf_dir and udf for piggy extractors")
            case _ =>
          }
          extractor.copy(
            outputRelation = extractorConfig.getString("output_relation"),
            inputQuery = InputQueryParser.parseInputQuery(extractorConfig.getString("input")),
            udfDir = udfDir orNull,
            udf = udf,
            loader = extractorConfig.getString("loader"),
            loaderConfig = Try(extractorConfig.getConfig("loader_config")).toOption map {
              case loaderConfigObj => LoaderConfig(
                connection = loaderConfigObj.getString("connection"),
                schemaFile = loaderConfigObj.getString("schema"),
                // TODO instead of specifying default values here, try to augment settings.config
                threads = Try(loaderConfigObj.getInt("threads")) getOrElse(extractor.parallelism),
                parallelTransactions = Try(loaderConfigObj.getInt("parallel_transactions")) getOrElse(60)
              )
            } orNull
          )

        case "sql_extractor" =>
          extractor.copy(
            sqlQuery = extractorConfig.getString("sql")
          )

        case "cmd_extractor" =>
          extractor.copy(
            cmd = Try(extractorConfig.getString("cmd")) toOption
          )

        case _ =>
          sys.error(s"${style}: Unrecognized extractor style")
      }
    }
    settings updatedConfig(config) copy(
      extractionSettings = ExtractionSettings(
        extractors = extractors toList,
        parallelism = config.getInt("extraction.parallelism")
      )
    )
  }

  private def loadInferenceSettings(settings: Settings): Settings = {
    var config = settings.config withFallback ConfigFactory.parseString(
      """
        |inference {
        |  skip_learning: false
        |  weight_table: ""
        |  parallel_grounding: false
        |  batch_size: null
        |  factors {
        |  }
        |}
      """.stripMargin)
    val skipLearning = config.getBoolean("inference.skip_learning")
    val weightTable = config.getString("inference.weight_table")
    val parallelGrounding = config.getBoolean("inference.parallel_grounding")
    val batchSize = Try(config.getInt("inference.batch_size")).toOption
    val factors = configEntries(config.getConfig("inference.factors")) map {
      case (factorName, factorConfigRaw) =>
        val factorConfig = factorConfigRaw withFallback ConfigFactory.parseString(
          s"""
            |input_query: null
            |function: null
            |weight: null
            |weightPrefix: ${factorName}
            |mode: "deepdive"
            |cnn_configurations: []
          """.stripMargin)
        config = config.withValue(s"inference.factors.${factorName}", factorConfig.root)
        FactorDesc(
          name = factorName,
          inputQuery = InputQueryParser.parseDatastoreInputQuery(factorConfig.getString("input_query")).query,
          func = FactorFunctionParser.parseFactorFunction(factorConfig.getString("function")),
          weight = FactorWeightParser.parseFactorWeight(factorConfig.getString("weight")),
          weightPrefix = factorConfig.getString("weightPrefix"),
          mode = Try(factorConfig.getString("mode")).toOption,
          cnnConfig = factorConfig.getStringList("cnn_configurations") toList,
          port = Try(factorConfig.getInt("port")).toOption
        )
    }
    settings updatedConfig(config) copy(
      inferenceSettings = InferenceSettings(
        factors = factors toList,
        insertBatchSize = batchSize,
        skipLearning = skipLearning,
        weightTable = weightTable,
        parallelGrounding = parallelGrounding
      )
    )
  }

  private def loadCalibrationSettings(settings: Settings): Settings = {
    val config = settings.config withFallback ConfigFactory.parseString(
      """
        |calibration {
        |  holdout_fraction: 0.0
        |  holdout_query: null
        |  observation_query: null
        |}
      """.stripMargin)
    settings updatedConfig(config) copy(
      calibrationSettings = CalibrationSettings(
        holdoutFraction = config.getDouble("calibration.holdout_fraction"),
        holdoutQuery = Try(config.getString("calibration.holdout_query")) toOption,
        observationQuery = Try(config.getString("calibration.observation_query")) toOption
      )
    )
  }

  private def loadSamplerSettings(settings: Settings): Settings = {
    val config = settings.config withFallback ConfigFactory.parseString(
      """
        |sampler {
        |  sampler_cmd: "sampler-dw"
        |  sampler_args: "-l 300 -s 1 -i 500 --alpha 0.1"
        |}
        |
        |inference.skip_learning: false
      """.stripMargin)
    settings updatedConfig(config) copy(
      samplerSettings = SamplerSettings(
        samplerCmd = config.getString("sampler.sampler_cmd"),
        samplerArgs = {
          val args = config.getString("sampler.sampler_args")
          if (!config.getBoolean("inference.skip_learning")) args
          else args.replaceAll( """-l +[0-9]+ *""", """-l 0 """)
        }
      )
    )
  }

  private def loadPipelineSettings(settings: Settings): Settings = {
    val config = settings.config withFallback ConfigFactory.parseString(
      """
        |pipeline {
        |  run: null
        |  pipelines {
        |  }
        |  relearn_from: null
        |  base_dir: null
        |}
      """.stripMargin)
    val relearnFrom = Try(config.getString("pipeline.relearn_from")).toOption
    settings updatedConfig(config) copy(
      pipelineSettings = PipelineSettings(
        activePipelineName = Try(config.getString("pipeline.run")) toOption,
        pipelines =
          // TODO move this logic out of this parser
          if (relearnFrom isDefined) Nil else {
            val pipelineConfig = config.getConfig("pipeline.pipelines")
            pipelineConfig.root.keys.toList.sorted.map { pipelineName =>
              Pipeline(
                id = pipelineName,
                tasks = pipelineConfig.getStringList(pipelineName) toSet
              )
            }
          },
        relearnFrom = relearnFrom orNull,
        baseDir = Try(config.getString("pipeline.base_dir")) toOption
      )
    )
  }
}

object DataTypeParser extends RegexParsers {
  def CategoricalParser = "Categorical" ~> "(" ~> """\d+""".r <~ ")" ^^ { n => MultinomialType(n.toInt) }

  def BooleanParser = "Boolean" ^^ { s => BooleanType }

  def dataType = CategoricalParser | BooleanParser

  def parseVariableType(dataTypeStr: String): VariableDataType with Product with Serializable = {
    DataTypeParser.parse(DataTypeParser.dataType, dataTypeStr).getOrElse {
      sys.error(s"Unknown data type: ${dataTypeStr}")
    }
  }

}

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

  def factorVariable = ("!" ?) ~ rep1sep(relationOrField, ".") ~ (arrayDefinition ?) ~
    (("=" ~> equalPredicate) ?) ^^ {
    case (isNegated ~ varList ~ isArray ~ predicate) =>
      FactorFunctionVariable(
        relation = varList.take(varList.size - 1).mkString("."),
        field = varList.last,
        isArray = isArray.isDefined,
        isNegated = isNegated.isDefined,
        predicate = readLong(predicate)
      )
  }

  def readLong(predicate: Option[String]): Option[Long] = {
    predicate match {
      case Some(number) => Some(number.toLong)
      case None => None
    }
  }

  def factorFunc = implyFactorFunction | orFactorFunction | andFactorFunction |
    equalFactorFunction | isTrueFactorFunction | xorFactorFunction | multinomialFactorFunction |
    linearFactorFunction | ratioFactorFunction | logicalFactorFunction


  def parseFactorFunction(factorFunction: String): FactorFunction with Product with Serializable = {
    FactorFunctionParser.parse(
      FactorFunctionParser.factorFunc, factorFunction).getOrElse {
      sys.error(s"parsing ${factorFunction} failed")
    }
  }
}

object FactorWeightParser extends RegexParsers {
  def relationOrField = """[^,()]+""".r

  def weightVariable = relationOrField

  def constantWeight = """-?[\d\.]+""".r ^^ { x => KnownFactorWeight(x.toDouble) }

  def unknownWeight = "?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }

  def factorWeight = constantWeight | unknownWeight

  def parseFactorWeight(factorWeightExpr: String): FactorWeight with Product with Serializable = {
    FactorWeightParser.parse(
      FactorWeightParser.factorWeight, factorWeightExpr).getOrElse {
      sys.error(s"parsing ${factorWeightExpr} failed")
    }
  }
}

object InputQueryParser extends RegexParsers {

  def filenameExpr = "'" ~> """[^']+""".r <~ "'"

  def CSVInputQueryExpr = "CSV" ~> "(" ~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, ',') }

  def TSVInputQueryExpr = "TSV" ~> "(" ~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, '\t') }

  def DatastoreInputQueryExpr = not("CSV") ~> not("TSV") ~> "[\\w\\W]+".r ^^ { str =>
    val withoutColon = """;\s+\n?$""".r.replaceAllIn(str, "")
    val result = """[\s\n]+""".r replaceAllIn(withoutColon, " ")
    DatastoreInputQuery(result)
  }

  def inputQueryExpr = (CSVInputQueryExpr | TSVInputQueryExpr | DatastoreInputQueryExpr)

  def parseDatastoreInputQuery(inputQuery: String): DatastoreInputQuery = {
    InputQueryParser.parse(InputQueryParser.DatastoreInputQueryExpr, inputQuery).getOrElse {
      sys.error(s"parsing ${inputQuery} failed")
    }
  }

  def parseInputQuery(inputQuery: String): InputQuery = {
    InputQueryParser.parse(InputQueryParser.inputQueryExpr, inputQuery).getOrElse {
      sys.error(s"parsing ${inputQuery} failed")
    }
  }
}

