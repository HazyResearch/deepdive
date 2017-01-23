package org.deepdive.ddlog

// DeepDiveLog compiler
// See: https://docs.google.com/document/d/1SBIvvki3mnR28Mf0Pkin9w9mWNam5AA0SpIGj1ZN2c4

import scala.collection.immutable.HashMap
import scala.collection.mutable.HashSet
import scala.language.postfixOps

// Compiler that takes parsed program as input and turns into blocks of deepdive.conf
class DeepDiveLogCompiler( program : DeepDiveLog.Program, config : DeepDiveLog.Config ) {
  import DeepDiveLogCompiler._

  val statements = program

  val attrNameForRelationAndPosition: Map[ Tuple2[String,Int], String ] =
    (statements collect {
      case decl: SchemaDeclaration =>
        decl.a.terms.zipWithIndex.map { case (n, i) => (decl.a.name, i) -> n }
    } flatten) toMap

  val functionDeclarationByName: Map[String, FunctionDeclaration] =
    statements collect {
      case fdecl: FunctionDeclaration => fdecl.functionName -> fdecl
    } toMap

  val schemaDeclarationByRelationName: Map[String, SchemaDeclaration] =
    statements collect {
      case decl: SchemaDeclaration => decl.a.name -> decl
    } toMap

  def isCategoricalRelation(name: String): Boolean =
    schemaDeclarationByRelationName get(name) map (_.categoricalColumns.size > 0) getOrElse(false)

  val inferenceRules = statements collect { case s: InferenceRule => s }

  val statementsByHeadName: Map[String, List[Statement]] =
    statements collect {
      case s: FunctionCallRule     => s.output -> s
      case s: ExtractionRule       => s.headName -> s
      case s: SupervisionRule      => s.headName -> s
    } groupBy(_._1) mapValues(_ map {_._2})

  val statementsByRepresentative: Map[Statement, List[Statement]] =
    statementsByHeadName map { case (name, stmts) => stmts(0) -> stmts }

  val representativeStatements : Set[Statement] =
    statementsByHeadName.values map { _(0) } toSet

  def slugify(input: String): String = {
    import java.text.Normalizer
    Normalizer.normalize(input, Normalizer.Form.NFD)
      .replaceAll("[^\\w\\s-]", "") // Remove all non-word, non-space or non-dash characters
      .replace('-', ' ')            // Replace dashes with spaces
      .trim                         // Trim leading/trailing whitespace (including what used to be leading/trailing dashes)
      .replaceAll("\\s+", "_")      // Replace whitespace (including newlines and repetitions) with underscore
      .toLowerCase                  // Lowercase the final results
  }

  def optionalIndex(idx: Int) =
    if (idx < 1) "" else s"${idx}"

  // Given a statement, resolve its name for the compiled extractor block.
  def resolveExtractorBlockName(s: Statement): String = s match {
    case d: SchemaDeclaration => s"init_${d.a.name}"
    case f: FunctionCallRule => s"ext${
      // Extra numbering is necessary because there can be many function calls to the same head relation
      // XXX This comes right after the prefix to prevent potential collision with user's name
      val functionCallsForSameOutputAndFunction =
        statementsByHeadName getOrElse(f.output, List.empty) filter {
          case s:FunctionCallRule => s.function == f.function
          case _ => false
        }
      val idx = functionCallsForSameOutputAndFunction indexOf f
      optionalIndex(idx)
    }_${f.output}_by_${f.function}"
    case e: ExtractionRule => s"ext_${e.headName}"
    case s: SupervisionRule => s"ext_${s.headName}"
    case _ => sys.error(s"${s}: Cannot determine extractor block name for rule")
  }

  // Given an inference rule, resolve its name for the compiled inference block.
  def resolveInferenceBlockName(s: InferenceRule): String = {
    // how to map a rule to its basename
    def ruleBaseNameFor(s: InferenceRule): String =
      if (s.ruleName nonEmpty) {
        slugify(s.ruleName getOrElse "rule")
      } else {
        s"inf_${
          // function name
          s.head.function.getClass.getSimpleName.toLowerCase
        }_${
          // followed by variable names
          s.head.variables map { case t => s"${if (t.isNegated) "not_" else ""}${t.atom.name}" } mkString("_")
        }"
      }
    // find this rule's base name and all rules that share it
    val ruleBaseName = ruleBaseNameFor(s)
    val allInferenceRulesSharingHead = inferenceRules filter(ruleBaseNameFor(_) equals ruleBaseName)
    if (allInferenceRulesSharingHead.length == 1) ruleBaseName // no possible name collision
    else if (s.ruleName nonEmpty)
      sys.error(s"""@name("${s.ruleName get}") repeated ${allInferenceRulesSharingHead.length} times""")
    else {
      // keep an index after the base name to prevent collision
      s"${
        // TODO finding safe prefix for every inference rule ahead of time will be more efficient
        DeepDiveLogDesugarRewriter.findSafePrefix(ruleBaseName, inferenceRules map ruleBaseNameFor toSet)
      }${
        allInferenceRulesSharingHead indexOf s
      }"
    }
  }

  // Given a variable, resolve it.  TODO: This should give a warning,
  // if we encouter a variable that is not in this map, then something
  // odd has happened.
  def resolveName( v : Variable ) : String = {
    v match { case Variable(v,relName,i) =>
      val realRel = relName.stripPrefix(deepdivePrefixForVariablesWithIdsTable)
      if(attrNameForRelationAndPosition contains (realRel,i)) {
        attrNameForRelationAndPosition(realRel,i)
      } else {
        // for views, columns names are in the form of "column_index"
        return s"column_${i}"
      }
    }
  }

  def collectUsedRelations1(body: Body): List[String] = body match {
    case a: Atom => List(a.name)
    case q: QuantifiedBody => collectUsedRelations(q.bodies)
    case _ => List.empty
  }
  def collectUsedRelations(bodies: List[Body]): List[String] = bodies flatMap collectUsedRelations1
  val relationNamesUsedByStatement: Map[Statement, List[String]] = statements collect {
      case f : FunctionCallRule => f -> (f.q.bodies flatMap collectUsedRelations distinct)
      case e : ExtractionRule   => e -> (e.q.bodies flatMap collectUsedRelations distinct)
      case e : SupervisionRule  => e -> (e.q.bodies flatMap collectUsedRelations distinct)
      case e : InferenceRule    => e -> (e.q.bodies flatMap collectUsedRelations distinct)
    } toMap


  sealed trait AliasStyle
  // ViewStyle: column_{columnIndex}
  case object ViewAlias extends AliasStyle
  // TableStyle: tableName.R{tableIndex}.columnName
  case object TableAlias extends AliasStyle
  // UseVariableAsAlias: to use the expression
  case object UseVariableAsAlias extends AliasStyle


  // This is responsible for compiling a single conjunctive query
class QueryCompiler(cq : ConjunctiveQuery, hackFrom: List[String] = Nil, hackWhere: List[String] = Nil) {
  // TODO remove var and make the generate* stuff below side-effect free
  // This is responsible for schema elements within a given query, e.g.,
  // what is the canonical version of x? (i.e., the first time it is
  // mentioned in the body. This is useful to translate to SQL (join
  // conditions, select, etc.)
  var query_schema = new HashMap[ String, Tuple2[String,Variable] ]()

  // Set of all symbols ever unified with a categorical column
  var categorical_val_vars = new HashSet[String]()

  // maps each variable name to a canonical version of itself (first occurrence in body in left-to-right order)
  // index is the index of the subgoal/atom this variable is found in the body.
  // variable is the complete Variable type for the found variable.
  def generateCanonicalVar()  = {
    def generateCanonicalVarFromAtom(a: Atom, index: String) {
      a.terms.zipWithIndex.foreach { case (expr, i) =>
        expr match {
          case VarPattern(v) =>
            schemaDeclarationByRelationName get(a.name) map {
              case decl =>
                if (decl.categoricalColumnIndexes contains i)
                  categorical_val_vars += v
            }

            if (! (query_schema contains v) )
              query_schema += { v -> (index, Variable(v, a.name, i) ) }
          case _ =>
        }
      }
    }
    // For quantified body, we need to recursively add variables from the atoms inside the quantified body
    def generateCanonicalVarFromBodies(bodies: List[Body], indexPrefix: String) {
      bodies.zipWithIndex.foreach {
        case (a: Atom, index) => generateCanonicalVarFromAtom(a, s"${indexPrefix}${index}")
        // we need to recursively handle the atoms inside the modifier
        case (a: QuantifiedBody, index) => generateCanonicalVarFromBodies(a.bodies, s"${indexPrefix}${index}_")
        case _ =>
      }
    }

    cq.bodies foreach { generateCanonicalVarFromBodies(_, "") }
  }
  generateCanonicalVar() // initialize

  // accessors
  def getBodyIndex( varName : String )       = { query_schema(varName)._1 }
  def getVar(varName : String ) : Variable   = { query_schema(varName)._2 }

  // compile a variable name to its canonical relation and column in R{index}.{name} style
  def compileVariable(v: String) = {
    val index = getBodyIndex(v)
    val name  = resolveName(getVar(v))
    s"R${index}.${name}"
  }

  // compile alias
  def compileAlias(e: Expr, a: Option[String], aliasStyle: AliasStyle) = aliasStyle match {
    case TableAlias => e match {
      case VarExpr(v) => {
        val index = getBodyIndex(v)
        val variable = getVar(v)
        val name  = resolveName(variable)
        val relation = variable.relName
        s""" AS "${relation}.R${index}.${name}\""""
      }
      case _ => s" AS ${deepdiveViewOrderedColumnPrefix}${cq.headTerms indexOf e}"
    }
    case ViewAlias => s" AS ${deepdiveViewOrderedColumnPrefix}${cq.headTerms indexOf e}"
    case UseVariableAsAlias => s""" AS "${DeepDiveLogPrettyPrinter.print(e)}\""""
  }

  def isCategoricalColumn(e: Expr) : Boolean = {
    e match {
      case VarExpr(name) => categorical_val_vars contains name
      case _ => false
    }
  }

  // resolve an expression
  def compileExpr(e: Expr) : String = compileExpr(e, 0)
  def compileExpr(e: Expr, level: Int) : String = {
    e match {
      case VarExpr(name) => compileVariable(name)
      case NullConst() => "NULL"
      case IntConst(value) => value.toString
      case DoubleConst(value) => value.toString
      case BooleanConst(value) => value.toString
      case StringConst(value) => s"'${value.replaceAll("'", "''")}'"
      case FuncExpr(function, args, agg) => {
        val resolvedArgs = args map (x => compileExpr(x))
        val resolved = s"${function}(${resolvedArgs.mkString(", ")})"
        resolved
      }
      case BinaryOpExpr(lhs, op, rhs) => {
        val resovledLhs = compileExpr(lhs, level + 1)
        val resovledRhs = compileExpr(rhs, level + 1)
        val sql = s"${resovledLhs} ${op} ${resovledRhs}"
        if (level == 0) sql else s"(${sql})"
      }
      case TypecastExpr(lhs, rhs) => {
        val resovledLhs = compileExpr(lhs)
        s"(${resovledLhs} :: ${rhs})"
      }
      case IfThenElseExpr(ifCondThenExprPairs, optElseExpr) => {
        (ifCondThenExprPairs map {
          case (ifCond, thenExpr) => s"WHEN ${compileCond(ifCond)} THEN ${compileExpr(thenExpr)}"
        }) ++ List(optElseExpr map compileExpr mkString("ELSE ", "", ""))
      } mkString("\nCASE ", "\n     ", "\nEND")
      case ArrayElementExpr(name, index) => s"${compileExpr(name, level + 1)}[${compileExpr(index, level + 1)}]"
    }
  }

  // resolve a condition
  def compileCond(cond: Cond) : String = compileCond(cond, 0)
  def compileCond(cond: Cond, level: Int) : String = {
    cond match {
      case ExprCond(e) => compileExpr(e)
      case NegationCond(c) => s"NOT ${compileCond(c, level + 1)}"
      case CompoundCond(lhs, op, rhs) => {
        val resolvedLhs = s"${compileCond(lhs, level + 1)}"
        val resolvedRhs = s"${compileCond(rhs, level + 1)}"
        val sql = op match {
          case LogicOperator.AND() => s"${resolvedLhs} AND ${resolvedRhs}"
          case LogicOperator.OR()  => s"${resolvedLhs} OR ${resolvedRhs}"
        }
        if (level == 0) sql else s"(${sql})"
      }
      case _ => ""
    }
  }

  // generate SQL SELECT cluase
  def generateSQLHead(aliasStyle: AliasStyle) = {
    val head = if (cq.headTerms isEmpty) "*" else cq.headTerms.zip(
      cq.headTermAliases getOrElse {cq.headTerms map {_ => None}}) map { case (expr, alias) =>
      compileExpr(expr) + compileAlias(expr, alias, aliasStyle)
    } mkString("\n     , ")
    val distinctStr = if (cq.isDistinct) "DISTINCT " else ""
    s"${distinctStr}${head}"
  }

  // This is generic code that generates the FROM with positional aliasing R0, R1, etc.
  // and the corresponding WHERE clause
  def generateSQLBody(body: List[Body]) : String = {
    // generate where clause from unification and conditions
    def generateWhereClause(bodies: List[Body], indexPrefix: String) : String = {
      val conditions = (bodies.zipWithIndex.flatMap {
        case (Atom(relName, terms), index) => {
          val bodyIndex = s"${indexPrefix}${index}"
          terms.zipWithIndex flatMap { case (pattern, termIndex) =>
            pattern match {
              // a simple variable indicates a join condition with other columns having the same variable name
              case VarPattern(varName) => {
                val canonical_body_index = getBodyIndex(varName)
                if (canonical_body_index != bodyIndex) {
                  val real_attr_name1 = resolveName( Variable(varName, relName, termIndex) )
                  val real_attr_name2 = resolveName( getVar(varName))
                  Some(s"R${ bodyIndex }.${ real_attr_name1 } = R${ canonical_body_index }.${ real_attr_name2 }")
                } else { None }
              }
              case PlaceholderPattern() => None
              // expression patterns indicate a filter condition on the column
              case ExprPattern(expr) => {
                val resolved = compileExpr(expr)
                val realRel = relName.stripPrefix(deepdivePrefixForVariablesWithIdsTable)
                val attr = attrNameForRelationAndPosition(realRel, termIndex)
                Some(s"R${bodyIndex}.${attr} = ${resolved}")
              }
            }
          }
        }
        case (x: Cond, _) => Some(compileCond(x, 1))
        case (x: QuantifiedBody, index) => {
          val newIndexPrefix = s"${indexPrefix}${index}_"
          val subqueryWhereStr = generateWhereClause(x.bodies, newIndexPrefix)
          val subqueryFromStr  = generateFromClause(x.bodies, newIndexPrefix)
          x.modifier match {
            case ExistModifier(negated) => Some(s"${if (negated) "NOT " else ""}EXISTS (SELECT 1 FROM ${subqueryFromStr} WHERE ${subqueryWhereStr})")
            case AllModifier() => Some(s"NOT EXISTS (SELECT 1 FROM ${subqueryFromStr} WHERE NOT (${subqueryWhereStr}))")
            case _ => None
          }
        }
      }) ::: hackWhere
      conditions.mkString("\n  AND ")
    }

    def generateFromClause(bodies: List[Body], indexPrefix: String) : String = {
      val atoms = (bodies.zipWithIndex flatMap {
        case (x:Atom,i) => Some(s"${x.name} R${indexPrefix}${i}")
        case _ => None
      }) ::: hackFrom
      val outers = bodies.zipWithIndex flatMap {
        case (x:QuantifiedBody,i) => {
          val newIndexPrefix = s"${indexPrefix}${i}_"
          x.modifier match {
            case OuterModifier() => {
              val from = generateFromClause(x.bodies, newIndexPrefix)
              val joinCond = generateWhereClause(x.bodies, newIndexPrefix)
              Some(s"${from}\n  ON ${joinCond}")
            }
            case _ => None
          }
        }
        case _ => None
      }
      // full outer join
      if (atoms isEmpty) {
        outers.mkString("\nFULL OUTER JOIN ")
      } else {
        (atoms.mkString("\n   , ") +: outers).mkString("\nLEFT OUTER JOIN ")
      }
    }

    def optionalClause(prefix: String, clause: String): String =
      if (clause.isEmpty) "" else prefix + " " + clause

    // check if an expression contains an aggregation function
    def containsAggregation(expr: Expr) : Boolean = {
      expr match {
        case FuncExpr(function, args, agg) => if (agg) agg else {
          args.map(containsAggregation).foldLeft(false)(_ || _)
        }
        case BinaryOpExpr(lhs, op, rhs) => containsAggregation(lhs) || containsAggregation(rhs)
        case TypecastExpr(lhs, rhs) => containsAggregation(lhs)
        case _ => false
      }
    }

    // handle group by
    // map head terms, leaving out aggregation functions
    val groupbyTerms = cq.headTerms.filterNot(containsAggregation) map (compileExpr(_))

    val groupbyStr = if (groupbyTerms.size == cq.headTerms.size || groupbyTerms.isEmpty) {
      ""
    } else {
      s"\nGROUP BY ${groupbyTerms.mkString("\n    , ")}"
    }

    // limit clause
    val limitStr = optionalClause("\nLIMIT", cq.limit.getOrElse("").toString)

    // order by clause by handling any @order_by annotations
    val orderbyStr =
      if (cq.headTermAnnotations.size != cq.headTerms.size) "" // invalid annotations
      else {
        val orderByAnnos = cq.headTermAnnotations map (_ find (_ named "order_by"))
        val orderByExprs = cq.headTerms.zip(orderByAnnos).zipWithIndex collect {
          case ((e, Some(orderByAnno)), i) =>
            val isAscending = // the "dir" or first argument of the annotation decides the sort order, defaulting to ASCending
              (orderByAnno.valueAt("dir", 0) getOrElse "ASC") == "ASC"
            val priority = // the priority or second argument decides the key index
              orderByAnno.valueAt("priority", 1) map (_.toString.toInt) getOrElse (i+1)
            (e, isAscending, priority)
        } sortBy (_._3)
        optionalClause("\nORDER BY", orderByExprs map {
            case (e, isAscending, _) => s"${compileExpr(e)} ${if (isAscending) "ASC" else "DESC"}"
          } mkString("\n    , ")
        )
      }

    s"""FROM ${ generateFromClause(body, "")
    }${optionalClause("\nWHERE", generateWhereClause(body, ""))
    }${groupbyStr
    }${orderbyStr
    }${limitStr
    }"""
  }

  def generateSQL(aliasStyle: AliasStyle = ViewAlias) = {
    val head = generateSQLHead(aliasStyle)
    val body = cq.bodies map generateSQLBody
    body map { b => s"SELECT ${head}\n${b}" } mkString("\nUNION ALL\n")
  }

}

  def escape4sh(arg: String): String = s"'${arg.replaceAll("'", "'\\\\''")}'"

  def compile(stmt: SchemaDeclaration): CompiledBlocks = {
    // Nothing to compile
    List.empty
  }

  def compile(stmt: FunctionDeclaration): CompiledBlocks = {
    // Nothing to compile
    List.empty
  }

  // Generate extraction rule part for deepdive
  def compile(stmt: ExtractionRule): CompiledBlocks = {
    val stmts = statementsByRepresentative getOrElse(stmt, List.empty) collect { case s: ExtractionRule => s }
    if (stmts isEmpty) return List.empty

    compileExtractorBlock(stmt, stmts, stmt.headName, stmts map (_.q))
  }

  def compileExtractorBlock(stmt: Statement, stmts: List[Statement], outputRelation: String, cqs: List[ConjunctiveQuery]): CompiledBlocks = {
    // look for @materialize annotation on the schema declaration or any rule defining the same head
    val shouldMaterialize = ((schemaDeclarationByRelationName get outputRelation) ++ stmts
      ) flatMap (_.annotations) exists (_ named "materialize")
    List(s"deepdive.extraction.extractors.${resolveExtractorBlockName(stmt)}" -> Map(
      "style" -> "sql_extractor",
      "input_relations" -> (stmts flatMap relationNamesUsedByStatement distinct),
      "output_relation" -> outputRelation,
      "materialize" -> shouldMaterialize,
      "sql" -> QuotedString("\n"+ (
          cqs map { q => new QueryCompiler(q).generateSQL() } mkString "\nUNION ALL\n"
        ) +"\n")
    ))
  }

  def compile(stmt: SupervisionRule): CompiledBlocks = {
    val stmts = statementsByRepresentative getOrElse(stmt, List.empty) collect { case s: SupervisionRule => s }
    if (stmts isEmpty) return List.empty

    compileExtractorBlock(stmt, stmts, stmt.headName,
      stmts map { case stmt => stmt.q.copy(
        headTerms = stmt.q.headTerms :+ stmt.supervision :+ (stmt.truthiness.getOrElse(DoubleConst(1))),
        headTermAliases = {
          stmt.q.headTermAliases orElse { Some(stmt.q.headTerms map (_ => None)) } map { _ ::: List(Some(deepdiveVariableLabelColumn), Some(deepdiveVariableLabelTruthinessColumn)) }
        },
        isDistinct = false // XXX no need to take DISTINCT here since DeepDive will have to do it anyway
      ) }
    )
  }

  def compile(stmt: FunctionCallRule): CompiledBlocks = {
    val stmts = statementsByRepresentative getOrElse(stmt, List.empty) collect { case s: FunctionCallRule => s }
    if (stmts isEmpty) return List.empty

    stmts map { case stmt =>
      val function = functionDeclarationByName(stmt.function)
      val udfDetails = function.implementations collectFirst {
        case impl: RowWiseLineHandler => List(
            "udf" -> QuotedString(impl.command),
            "style" -> s"${impl.style}_extractor"
          )
      }

      if (udfDetails.isEmpty)
        sys.error(s"Cannot find compilable implementation for function ${stmt.function} among:\n  "
          + (function.implementations mkString "\n  "))

      val blockName = resolveExtractorBlockName(stmt)
      val parallelism = stmt.annotations find (_ named "parallelism") flatMap (_.value) getOrElse("${PARALLELISM}")
      s"deepdive.extraction.extractors.${blockName}" -> (Map(
        "input" -> QuotedString("\n"+ new QueryCompiler(stmt.q).generateSQL() +"\n"),
        "output_relation" -> stmt.output,
        "input_relations" -> (List(stmt) flatMap relationNamesUsedByStatement distinct),
        "input_batch_size" -> "${INPUT_BATCH_SIZE}",
        "parallelism" -> parallelism
        ) ++ udfDetails.get)
    }
  }

  // generate inference rule part for deepdive
  def compile(stmt: InferenceRule): CompiledBlocks = {
    val headAsBody = stmt.head.variables map (_.atom)
    val variableIdAndColumns = headAsBody.zipWithIndex flatMap {
      case (x: Atom, i) if schemaDeclarationByRelationName get x.name exists (_.isQuery) =>
        // TODO maybe TableAlias can be useful here or we can completely get rid of it?
        // variable id column
        s"""R${headAsBody indexOf x}.${
          deepdiveVariableIdColumn
        } AS "${x.name}.R${headAsBody indexOf x}.${deepdiveVariableIdColumn}\"""" :: (
          // project variable key columns as well (to reduce unnecssary joins)
          schemaDeclarationByRelationName get x.name map (_.keyColumns map {
            case term => s"""R${headAsBody indexOf x}.${term
            } AS "${x.name}.R${headAsBody indexOf x}.${term}\""""
          }) get
        ) ++ (
          // project category value columns as well (to reduce unnecssary joins)
          schemaDeclarationByRelationName get x.name map (_.categoricalColumns map {
            case term => s"""R${headAsBody indexOf x}.${term
            } AS "${x.name}.R${headAsBody indexOf x}.${term}\""""
          }) get
        )

      case _ => List.empty
    }

    val headAsBodyWithIds = headAsBody map {
      case(x: Atom) => Atom(s"""${deepdivePrefixForVariablesWithIdsTable}${x.name}""", x.terms)
    }

    var nonCategoryWeightCols = new HashSet[String]()

    val inputQueries =
      stmt.q.bodies map { case cqBody =>
        // Here we need to select from the bodies atoms as well as the head atoms,
        // which are the variable relations.
        // This is achieved by puting head atoms into the body.
        val fakeBody        = headAsBodyWithIds ++ cqBody
        val fakeCQ          = stmt.q.copy(bodies = List(fakeBody))

        val qc = new QueryCompiler(fakeCQ)

        // weight columns
        val weightColumns = stmt.weights.variables.zipWithIndex collect {
          case (s: Expr, i) if !s.isInstanceOf[ConstExpr] =>
            s"""${qc.compileExpr(s)} AS "${deepdiveWeightColumnPrefix}${i}\""""
        }

        // nonCategoryWeightCols
        stmt.weights.variables.zipWithIndex.foreach {case(s: Expr, i) =>
          if (!s.isInstanceOf[ConstExpr] && !qc.isCategoricalColumn(s))
            nonCategoryWeightCols += s"${deepdiveWeightColumnPrefix}${i}"
        }

        val valueExpr = stmt.valueExpr getOrElse IntConst(1)
        val valueColumn = s"""(${qc.compileExpr(valueExpr)})::float AS feature_value"""

        // factor input query
        s"""SELECT ${(variableIdAndColumns ++ weightColumns ++ List(valueColumn)) mkString ("\n     , ")
        }\n${qc.generateSQLBody(fakeBody) }"""
      }

    // factor function
    val func = {
      val funcBody = headAsBody zip stmt.head.variables map { case (x, y) =>
        s"""${if (y.isNegated) "!" else ""}${x.name}.R${headAsBody indexOf x}.${deepdiveVariableLabelColumn}"""
      }

      val categoricalVars = stmt.head.variables filter { t => isCategoricalRelation(t.atom.name) }
      val isCategoricalAndFactor =
        if (categoricalVars.size == 0 || stmt.head.variables.size == categoricalVars.size)
          categoricalVars.size > 0
        else
          sys.error("None or all variables must be categorical")

      val function =
        if (isCategoricalAndFactor) {
          stmt.head.function match {
            case FactorFunction.IsTrue() | FactorFunction.And() => "AndCategorical"
            case f => sys.error(s"Unsupported factor over categorical variables: ${f}")
          }
        } else { // Boolean
          stmt.head.function match {
            case FactorFunction.Imply()  => "Imply"
            case FactorFunction.And()    => "And"
            case FactorFunction.Or()     => "Or"
            case FactorFunction.Equal()  => "Equal"
            case FactorFunction.Linear() => "Linear"
            case FactorFunction.Ratio()  => "Ratio"
            case FactorFunction.IsTrue() => "Imply" // TODO fix to IsTrue
          }
      }
      s"""${function}(${funcBody.mkString(", ")})"""
    }

    // weight
    val weight = {
      // note error cases should be handled in semantic checker
      // FIXME check if all weights.variables are ConstExpr to determine whether it's a fixed or unknown weight
      stmt.weights.variables(0) match {
        case IntConst(value) => value.toString
        case DoubleConst(value) => value.toString
        case StringConst(value) => "?"
        case _ => {
          val weightVars = stmt.weights.variables.zipWithIndex.flatMap {
            case(s: Expr, i) => Some(s"${deepdiveWeightColumnPrefix}${i}")
          } mkString(", ")
          s"?(${weightVars})"
        }
      }
    }

    List(s"deepdive.inference.factors.${resolveInferenceBlockName(stmt)}" -> Map(
        "input_query" -> QuotedString("\n"+inputQueries.mkString(" UNION ALL ")+"\n"),
        "function" -> QuotedString(func),
        "weight" -> QuotedString(weight),
        "non_category_weight_cols" -> nonCategoryWeightCols.toList.sorted,
        "input_relations" -> {
          val relationsInHead = stmt.head.variables map (_.atom.name)
          val relationsInBody = relationNamesUsedByStatement getOrElse(stmt, List.empty)
          (relationsInHead ++ relationsInBody) distinct
        }
    ))
  }

  def compile(stmt: Statement): CompiledBlocks = stmt match {
    case s: SchemaDeclaration => compile(s)
    case s: FunctionDeclaration => compile(s)
    case s: FunctionCallRule => compile(s)
    case s: ExtractionRule => compile(s)
    case s: SupervisionRule => compile(s)
    case s: InferenceRule => compile(s)
    case _ => sys.error(s"Compiler does not recognize statement: ${stmt}")
  }

  // generate variable schema statements
  def compileVariableSchema(): CompiledBlocks = {
    // generate the statements.
    List("deepdive.schema.variables" -> (statements collect {
      case decl: SchemaDeclaration if decl.isQuery =>
        if (isCategoricalRelation(decl.a.name)) {
          decl.a.name -> (decl.categoricalColumns map (_ -> "Categorical") toMap)
        } else // Boolean variables
          s"${decl.a.name}.${deepdiveVariableLabelColumn}" -> "Boolean"
      } toMap)
    )
  }

  def compile(): CompiledBlocks = (
    compileVariableSchema()
    ++
    (statements flatMap compile)
  )

}

object DeepDiveLogCompiler extends DeepDiveLogHandler {

  type CompiledBlocks = List[(String, Any)]  // TODO be more specific
  case class QuotedString(value: String)

  // some of the reserved names used in compilation
  val deepdiveViewOrderedColumnPrefix = "column_"
  val deepdiveWeightColumnPrefix = "dd_weight_column_"
  val deepdiveVariableIdColumn = "dd_id"
  val deepdiveVariableLabelColumn = "dd_label"
  val deepdiveVariableLabelTruthinessColumn = "dd_truthiness"
  val deepdivePrefixForVariablesWithIdsTable = "dd_variables_with_id_"

  // entry point for compilation
  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    // don't compile if it doesn't pass all semantic checks
    DeepDiveLogSemanticChecker.run(parsedProgram, config)
    val programToCompile = parsedProgram

    // take an initial pass to analyze the parsed program
    val compiler = new DeepDiveLogCompiler( programToCompile, config )

    // compile the program into blocks of deepdive.conf
    val blocks = compiler.compile()

    // how to codegen HOCON
    def codegenValue(value: Any): String = value match {
      case QuotedString(s) => // multi-line string
        s"""\"\"\"${s replaceAll("\"\"\"", "\\\"\\\"\\\"")}\"\"\""""
      case _ =>
        value.toString
    }
    def codegen(key: String, value: Any): String = value match {
      case m: Map[_, _] => // map
        s"""${key} {
           |${m map { case (k, v) => codegen(k.toString, v) } mkString("")}
           |}
           |""".stripMargin

      case l: List[_] => // lists
        s"""${key}: [${l map codegenValue mkString("\n  ", "\n  ", "")}
           |]
           |""".stripMargin
      case l: Set[_] => // sets
        s"""${key}: [${l map codegenValue mkString("\n  ", "\n  ", "")}
           |]
           |""".stripMargin

      case _ =>
        s"""${key}: ${codegenValue(value)}
           |""".stripMargin

    }

    // include the schema declarations before the compiled HOCON
    print("deepdive.schema ")
    DeepDiveLogSchemaExporter.run(parsedProgram, config)
    println()

    // codegen HOCON
    blocks foreach { case (key, value) => println(codegen(key, value)) }
  }

}
