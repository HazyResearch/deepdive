package org.deepdive.ddlog

// DeepDiveLog syntax
// See: https://docs.google.com/document/d/1SBIvvki3mnR28Mf0Pkin9w9mWNam5AA0SpIGj1ZN2c4

import org.apache.commons.lang3.StringEscapeUtils

import scala.language.postfixOps
import scala.util.parsing.combinator._

// ***************************************
// * The union types for for the parser. *
// ***************************************
case class Variable(varName : String, relName : String, index : Int )

sealed trait Expr
case class VarExpr(name: String) extends Expr
case class ArrayElementExpr(array: Expr, index: Expr) extends Expr
sealed trait ConstExpr extends Expr
{
  def value: Any
}
case class StringConst(value: String) extends ConstExpr
case class IntConst(value: Int) extends ConstExpr
case class DoubleConst(value: Double) extends ConstExpr
case class BooleanConst(value: Boolean) extends ConstExpr
case class NullConst() extends ConstExpr { def value = null }
case class FuncExpr(function: String, args: List[Expr], isAggregation: Boolean) extends Expr
case class BinaryOpExpr(lhs: Expr, op: String, rhs: Expr) extends Expr
case class TypecastExpr(lhs: Expr, rhs: String) extends Expr
case class IfThenElseExpr(ifCondThenExprPairs: List[(Cond, Expr)], elseExpr: Option[Expr]) extends Expr

sealed trait Pattern
case class VarPattern(name: String) extends Pattern
case class ExprPattern(expr: Expr) extends Pattern
case class PlaceholderPattern() extends Pattern

sealed trait Body
case class Atom(name : String, terms : List[Pattern]) extends Body
case class QuantifiedBody(modifier: BodyModifier, bodies: List[Body]) extends Body

sealed trait BodyModifier
case class ExistModifier(negated: Boolean) extends BodyModifier
case class OuterModifier() extends BodyModifier
case class AllModifier() extends BodyModifier

case class Attribute(name : String, terms : List[String], types : List[String], annotations : List[List[Annotation]])
case class ConjunctiveQuery(headTerms: List[Expr],
                            bodies: List[List[Body]],
                            isDistinct: Boolean = false,
                            limit: Option[Int] = None,
                            // optional annotations for head terms
                            headTermAnnotations: List[List[Annotation]] = List.empty,
                            headTermAliases: Option[List[Option[String]]] = None,
                            // XXX This flag is not ideal, but minimizes the impact of query treatment when compared to creating another case class
                            isForQuery: Boolean = false
                           )

case class Annotation( name : String // name of the annotation
                     , args : Option[Either[Map[String,Expr],List[Expr]]] = None // optional, named arguments map or unnamed list
                     )
{
  def named(n: String): Boolean = name.toLowerCase == n.toLowerCase

  def exprs: List[Expr] = args map (_ fold (_.values toList, es => es)) getOrElse List.empty
  def values: List[Any] = exprs collect { case e: ConstExpr => e.value }

  def expr: Option[Expr] = exprs headOption
  def value: Option[Any] = values headOption

  def exprAt(namedArg: String, positionalArg: Int): Option[Expr] =
    args flatMap (_ fold (_ get namedArg, _ lift positionalArg))
  def valueAt(namedArg: String, positionalArg: Int): Option[Any] =
    exprAt(namedArg, positionalArg) collect { case e: ConstExpr => e.value }
}

// condition
sealed trait Cond extends Body
case class ExprCond(expr: Expr) extends Cond
case class NegationCond(cond: Cond) extends Cond
case class CompoundCond(lhs: Cond, op: LogicOperator.LogicOperator, rhs: Cond) extends Cond

// logic operators
object LogicOperator {
  sealed trait LogicOperator
  case class AND() extends LogicOperator
  case class OR() extends LogicOperator
}

case class FactorWeight(variables: List[Expr])

// factor function
object FactorFunction {
  sealed trait FactorFunction
  case class IsTrue()      extends FactorFunction
  case class Imply()       extends FactorFunction
  case class Or()          extends FactorFunction
  case class And()         extends FactorFunction
  case class Equal()       extends FactorFunction
  case class Linear()      extends FactorFunction
  case class Ratio()       extends FactorFunction
}
case class HeadAtom(atom: Atom, isNegated: Boolean = false)
case class InferenceRuleHead(function: FactorFunction.FactorFunction, variables: List[HeadAtom])


sealed trait FunctionInputOutputType
case class RelationTypeDeclaration(names: List[String], types: List[String]) extends FunctionInputOutputType
case class RelationTypeAlias(likeRelationName: String) extends FunctionInputOutputType

sealed trait FunctionImplementationDeclaration
case class RowWiseLineHandler(style: String, command: String) extends FunctionImplementationDeclaration

// Statements that will be parsed and compiled
sealed trait Statement
{
  val annotations: List[Annotation]
}
case class SchemaDeclaration(a: Attribute,
                             isQuery: Boolean,
                             annotations: List[Annotation] = List.empty
                            ) extends Statement // atom and whether this is a query relation.
{
  // find all columns with @key annotation
  val keyColumns = {
    val columnsAnnotatedWithKey = a.terms.zip(a.annotations) collect {
      case (term, annos) if annos exists (_ named "key") => term
    }
    // treat all columns as key if none were annotated
    if (isQuery && (columnsAnnotatedWithKey isEmpty)) a.terms
    // otherwise, keys should be explicitly annotated
    else columnsAnnotatedWithKey
  }
  // this is a categorical variable if a strict subset of columns are annotated with @key
  val categoricalColumns =
    if (isQuery)
      a.terms diff keyColumns toList
    else
      List.empty

  val categoricalColumnIndexes : Set[Int] =
    if (isQuery)
      a.terms.zipWithIndex.filter { case(term, i) =>
        categoricalColumns contains term
      } map { _._2} toSet
    else
      Set.empty
}
case class FunctionDeclaration(functionName: String,
                               inputType: FunctionInputOutputType,
                               outputType: FunctionInputOutputType,
                               implementations: List[FunctionImplementationDeclaration],
                               annotations: List[Annotation] = List.empty
                              ) extends Statement
sealed trait RuleWithConjunctiveQuery extends Statement { val q: ConjunctiveQuery }
case class ExtractionRule(headName: String,
                          q: ConjunctiveQuery,
                          annotations: List[Annotation] = List.empty
                         ) extends RuleWithConjunctiveQuery
case class FunctionCallRule(output: String,
                            function: String,
                            q: ConjunctiveQuery,
                            annotations: List[Annotation] = List.empty
                           ) extends RuleWithConjunctiveQuery
case class SupervisionRule(headName: String,
                           supervision: Expr,
                           q: ConjunctiveQuery,
                           annotations: List[Annotation] = List.empty
                          ) extends RuleWithConjunctiveQuery
case class InferenceRule(head: InferenceRuleHead,
                         q: ConjunctiveQuery,
                         weights: FactorWeight = null,
                         annotations: List[Annotation] = List.empty
                        ) extends RuleWithConjunctiveQuery {

  val ruleName: Option[String] =
    annotations find (_ named "name") flatMap (_ expr) map {
      case StringConst(x) => x
      case _ => sys.error(s"Invalid rule name:\n${DeepDiveLogPrettyPrinter.print(this)}")
    }

  def valueExpr: Option[Expr] =
    annotations find (_ named "feature_value") flatMap (_ expr)

}












// Parser
class DeepDiveLogParser extends JavaTokenParsers {

  // JavaTokenParsers provides several useful number parsers:
  //   wholeNumber, decimalNumber, floatingPointNumber
  def double = opt("-") ~ """\d+\.\d+""".r ^^ { case (neg ~ num) => (neg.getOrElse("") + num).toDouble }
  def integer = opt("-") ~ wholeNumber ^^ { case (neg ~ num) => (neg.getOrElse("") + num).toInt }
  def stringLiteralAsString = stringLiteral ^^ {
    s => StringEscapeUtils.unescapeJava(
      s.stripPrefix("\"").stripSuffix("\""))
  } withFailureMessage "a string literal is expected"

  // Single-line comments beginning with # or // are supported by treating them as whiteSpace
  // C/Java/Scala style multi-line comments cannot be easily supported with RegexParsers unless we introduce a dedicated lexer.
  protected override val whiteSpace = """(?:(?:^|\s+)#.*|//.*|\s)+""".r

  // We just use Java identifiers to parse various names
  def relationName = ident named "relation name"
  def columnName   = ident named "column name"
  def columnType   = ident ~ ("[]"?) ^^ {
      case ty ~ isArrayType => ty + isArrayType.getOrElse("")
    } named "column type"
  def variableName = ident named "variable name"
  def functionName = ident named "function name"
  def functionModeType = stringLiteralAsString
  def inferenceModeType = stringLiteralAsString
  def annotationName = ident named "annotation name"
  def annotationArgumentName = ident named "annotation argument name"

  def annotation: Parser[Annotation] =
    "@" ~ annotationName ~ opt(annotationArguments) ^^ {
      case (_ ~ name ~ optArgs) => Annotation(name, optArgs)
    } named "annotation"

  def annotationArguments: Parser[Either[Map[String, Expr], List[Expr]]] =
    "(" ~> (
      // XXX rep1sep must be above repsep to parse "()"
      rep1sep(annotationArgumentValue, ",") ^^ {
        case values => Right(values)
      }
    |||
      repsep(annotationNamedArgument, ",") ^^ {
        case namedArgs => Left(namedArgs toMap)
      }
    ) <~ ")" named "annotation arguments"
  def annotationNamedArgument: Parser[(String, Expr)] =
    annotationArgumentName ~ "=" ~ annotationArgumentValue ^^ {
      case name ~ _ ~ value => name -> value
    } named "annotation named arguments"
  def annotationArgumentValue: Parser[Expr] = expr named "annotation argument value"

  def columnDeclaration: Parser[(String, String, List[Annotation])] =
    rep(annotation) ~
    columnName ~ columnType ^^ {
      case(anno ~ name ~ ty) => (name, ty, anno)
    } named "column declaration"

  def schemaDeclaration: Parser[SchemaDeclaration] =
    relationName ~ opt("?") ~ ("(" ~> rep1sep(columnDeclaration, ",") <~ ")") ^^ {
      case (r ~ isQuery ~ attrs) =>
        attrs unzip3 match { case (vars, types, annos) =>
          SchemaDeclaration(Attribute(r, vars, types, annos), isQuery != None)
        }
    } named "schema declaration"

  def functionInputOutputType: Parser[FunctionInputOutputType] =
    ( "rows" ~> "like" ~> relationName ^^ RelationTypeAlias
    | "(" ~> rep1sep(columnDeclaration, ",") <~ ")" ^^ { attrs =>
        attrs unzip3 match { case (names, types, annos) =>
          RelationTypeDeclaration(names, types)
        }
      }
    ) named "function input/output type declaration"

  def functionImplementation : Parser[FunctionImplementationDeclaration] =
    ( "implementation" ~ stringLiteralAsString ~ "handles" ~ ("tsj" | "tsv" | "json") ~ "lines" ^^ {
        case (_ ~ command ~ _ ~ style ~ _) => RowWiseLineHandler(command=command, style=style)
      }
    ) named "function implementation declaration"

  def functionDeclaration : Parser[FunctionDeclaration] =
    ( "function" ~ functionName ~ "over" ~ functionInputOutputType
    ~ "returns" ~ functionInputOutputType
    ~ (functionImplementation+)
    ) ^^ {
      case ("function" ~ a ~ "over" ~ inTy
        ~ "returns" ~ outTy
        ~ implementationDecls) =>
        FunctionDeclaration(a, inTy, outTy, implementationDecls)
    } named "function declaration"


  def operator =
    ( "||" | "+" | "-" | "*" | "/" | "&" | "%" ) named "operator"
  def typeOperator = "::"
  val aggregationFunctions = Set("MAX", "SUM", "MIN", "ARRAY_ACCUM", "ARRAY_AGG", "COUNT")

  // expression
  def expr : Parser[Expr] =
    ( lexpr ~ ("[" ~> expr <~ "]") ^^ { case (a ~ i) => ArrayElementExpr(a, i) }
    | lexpr ~ operator ~ expr ^^ { case (lhs ~ op ~ rhs) => BinaryOpExpr(lhs, op, rhs) }
    | lexpr ~ typeOperator ~ columnType ^^ { case (lhs ~ _ ~ rhs) => TypecastExpr(lhs, rhs) }
    | lexpr
    ) named "expression"

  def cexpr =
    ( expr ~ compareOperator ~ expr ^^ { case (lhs ~ op ~ rhs) => BinaryOpExpr(lhs, op, rhs) }
    | expr
    )

  def lexpr =
    ( "if" ~> (cond ~ ("then" ~> expr) ~ rep(elseIfExprs) ~ opt("else" ~> expr)) <~ "end" ^^ {
        case (ifCond ~ thenExpr ~ elseIfs ~ optElseExpr) =>
          IfThenElseExpr((ifCond, thenExpr) :: elseIfs, optElseExpr)
      }
    | stringLiteralAsString ^^ StringConst
    | double ^^ DoubleConst
    | integer ^^ IntConst
    | ("TRUE" | "FALSE") ^^ { x => BooleanConst(x.toBoolean) }
    | "NULL" ^^ { _ => NullConst() }
    | functionName ~ "(" ~ rep1sep(expr, ",") ~ ")" ^^ {
        case (name ~ _ ~ args ~ _) => FuncExpr(name, args, aggregationFunctions contains name.toUpperCase)
      }
    | variableName ^^ VarExpr
    | "(" ~> expr <~ ")"
    )

  def elseIfExprs =
    ("else" ~> "if" ~> cond) ~ ("then" ~> expr) ^^ {
      case (ifCond ~ thenExpr) => (ifCond, thenExpr)
    }

  // conditional expressions
  def compareOperator = ("LIKE" | ">=" | "<=" | "!=" | ">" | "<" | "=" | "IS" ~ "NOT" ^^ { _ => "IS NOT" } | "IS") named "compare operator"

  def cond : Parser[Cond] =
    ( acond ~ ";" ~ cond ^^ { case (lhs ~ op ~ rhs) =>
        CompoundCond(lhs, LogicOperator.OR(), rhs)
      }
    | acond
    ) named "conditional expression"
  def acond : Parser[Cond] =
    ( lcond ~ "," ~ acond ^^ { case (lhs ~ op ~ rhs) => CompoundCond(lhs, LogicOperator.AND(), rhs) }
    | lcond
    )
  // ! has higher priority...
  def lcond : Parser[Cond] =
    ( "!" ~> bcond ^^ NegationCond
    | bcond
    )
  def bcond : Parser[Cond] =
    ( cexpr ^^ ExprCond
    | "[" ~> cond <~ "]"
    )

  def pattern : Parser[Pattern] =
    ( "_"  ^^ { case _ => PlaceholderPattern() }
    | expr ^^ {
        case VarExpr(x) => VarPattern(x)
        case x: Expr => ExprPattern(x)
      }
    ) named "pattern"

  def atom: Parser[Atom] =
    relationName ~ ("(" ~> rep1sep(pattern, ",") <~ ")") ^^ {
      case n ~ ps => Atom(n, ps)
    }

  def quantifiedBody = (opt("!") ~ "EXISTS" | "OPTIONAL" | "ALL") ~ "[" ~ rep1sep(cqBody, ",") ~ "]" ^^ { case (m ~ _ ~ b ~ _) =>
    val modifier = m match {
      case (not ~ "EXISTS") => ExistModifier(not != None)
      case "OPTIONAL" => OuterModifier()
      case "ALL" => AllModifier()
    }
    QuantifiedBody(modifier, b)
  }

  def cqBody: Parser[Body] = quantifiedBody | atom | lcond

  def cqConjunctiveBody: Parser[List[Body]] = rep1sep(cqBody, ",")

  def conjunctiveQueryBody(separator: Parser[_] = ":-") : Parser[ConjunctiveQuery] =
    opt("*") ~ opt("|" ~> decimalNumber) ~ separator ~ rep1sep(cqConjunctiveBody, ";") ^^ {
      case (isDistinct ~ limit ~ _ ~ disjunctiveBodies) =>
        ConjunctiveQuery(List.empty, disjunctiveBodies, isDistinct isDefined, limit map (_.toInt))
    }

  def conjunctiveQueryHeadTerms = "(" ~> rep1sep(expr, ",") <~ ")"

  def conjunctiveQuery : Parser[ConjunctiveQuery] =
    // TODO fill headTermAnnotations as done in queryWithOptionalHeadTerms to support @order_by
    conjunctiveQueryHeadTerms ~ conjunctiveQueryBody() ^^ {
      case (head ~ cq) => cq.copy(headTerms = head)
    }

  def ruleWithConjunctiveQuery: Parser[RuleWithConjunctiveQuery] =
    ( // normal derivation rule (or could also be IsTrue factor rule when @weight annotation is present which is handled later)
      relationName ~ conjunctiveQuery ^^ {
        case head ~ cq =>
          ExtractionRule(headName = head, q = cq)
      }
    | // inference rule with special syntax in head for designated factors
      inferenceRuleHead ~ conjunctiveQueryBody() ^^ {
        case head ~ cq =>
          InferenceRule(head = head, q = cq)
      }
    | // supervision rule with equal sign
      relationName ~ conjunctiveQueryHeadTerms ~ ("=" ~> expr) ~ conjunctiveQueryBody() ^^ {
        case h ~ ts ~ sup ~ cq =>
          SupervisionRule(headName = h, supervision = sup, q = cq.copy(headTerms = ts))
      }
    | // function call with += sign between head and function call
      relationName ~ ("+=" ~> functionName) ~ conjunctiveQuery ^^ {
        case out ~ func ~ cq =>
          FunctionCallRule(output = out, function = func, q = cq)
      }
    )

  // factor functions
  def inferenceRuleHead =
    ( rep1sep(headAtom, ",") ~ ("=>" ~> headAtom) ^^ { case (a ~ b) => InferenceRuleHead(FactorFunction.Imply() ,  a :+ b) }
    |         headAtom    ~ rep1("=" ~> headAtom) ^^ { case (a ~ b) => InferenceRuleHead(FactorFunction.Equal() ,  a +: b) }
    |         headAtom    ~ rep1("^" ~> headAtom) ^^ { case (a ~ b) => InferenceRuleHead(FactorFunction.And()   ,  a +: b) }
    |         headAtom    ~ rep1("v" ~> headAtom) ^^ { case (a ~ b) => InferenceRuleHead(FactorFunction.Or()    ,  a +: b) }
    |         headAtom                            ^^ { case  a      => InferenceRuleHead(FactorFunction.IsTrue(), List(a)) }
    )
  def headAtom = opt("!") ~ atom ^^ {
    case isNegated ~ atom => HeadAtom(atom, isNegated isDefined)
  }

  // rules or schema elements in arbitrary order
  def statement : Parser[Statement] = (
    rep(annotation) ~
      ( schemaDeclaration
      | functionDeclaration
      | ruleWithConjunctiveQuery
      )
    ) ^^ {
    case annos ~ stmt if annos isEmpty => stmt
    case annos ~ stmt => stmt match {
      case s: SchemaDeclaration   => s.copy(annotations = annos)
      case s: FunctionDeclaration => s.copy(annotations = annos)
      case s: FunctionCallRule    => s.copy(annotations = annos)
      case s: ExtractionRule      => s.copy(annotations = annos)
      case s: SupervisionRule     => s.copy(annotations = annos)
      case s: InferenceRule       => s.copy(annotations = annos)
    }

  } ^? {
    // adjust parse result based on annotations
    case decl: SchemaDeclaration => decl
    case decl: FunctionDeclaration => decl
    case fncall: FunctionCallRule => fncall
    case supvis: SupervisionRule => supvis

    // treat @weight annotations
    case rule: RuleWithConjunctiveQuery if rule.annotations exists {
      anno => (anno named "weight") && (anno.args isDefined)
    } =>
      val weights = FactorWeight(rule.annotations find (_ named "weight") map (_ exprs) get)

      rule match {
        case extrRule: ExtractionRule =>
          // turn normal derivation rules to IsTrue factors
          val headAtom = HeadAtom(Atom(extrRule.headName, extrRule.q.headTerms map {
            case VarExpr(x) => VarPattern(x)
            case e => ExprPattern(e)
          }), isNegated = false)
          InferenceRule(
            head = InferenceRuleHead(FactorFunction.IsTrue(), List(headAtom)),
            q = extrRule.q.copy(headTerms = List.empty),
            weights = weights,
            annotations = extrRule.annotations
          )
        case infrRule: InferenceRule =>
          infrRule.copy(weights = weights)

        case _ => sys.error(s"Invalid usage of @weight:\n${DeepDiveLogPrettyPrinter.print(rule)}")
      }
      // DeepDiveLogPrettyPrinter.print(rule)

    // treat @label annotation
    case rule: ExtractionRule if rule.annotations exists {
      anno => (anno named "label") && (anno.exprs.size == 1)
    } =>
      val labelExpr = (rule.annotations find (_ named "label") flatMap (_ expr) get)
      SupervisionRule(headName = rule.headName, supervision = labelExpr, q = rule.q,
        annotations = rule.annotations filterNot (_ named "label"))

    case rule: ExtractionRule => rule
    // otherwise (InferenceRule without @weight), it's an error

  } ^? {
    // treat @semantics annotation
    case rule@InferenceRule(InferenceRuleHead(FactorFunction.Imply(), _), _, _, _) if rule.annotations exists {
      anno => (anno named "semantics") && (anno.exprs.size == 1)
    } =>
      rule.annotations find (_ named "semantics") flatMap (_ value) collect {
        case s: String => s.toLowerCase match {
          case "linear" => FactorFunction.Linear()
          case "ratio" => FactorFunction.Ratio()
          case "logical" => FactorFunction.Imply()
          case _ => sys.error(s"$s: unrecognized @semantics value")
        }
        case v => sys.error(s"${v}: unrecognized @semantics")
      } map {
        case factor => rule.copy(head = rule.head.copy(function = factor))
      } get
    case rule: Statement if !rule.isInstanceOf[InferenceRule] ||
      !(rule.annotations exists (_ named "semantics")) => rule

  }

  def program : Parser[DeepDiveLog.Program] = phrase(rep1(statement <~ "."))




  // query is a conjunctive query with optional projection and extraction rules
  def query : Parser[DeepDiveLog.Query] =
    rep(normalRule <~ ".") ~ (queryWithOptionalHeadTerms <~ ".") ^^ { case rules ~ q => (q, rules) }

  def normalRule: Parser[ExtractionRule] =
    relationName ~ conjunctiveQuery ^^ {
      case headName ~ cq =>
        ExtractionRule(headName = headName, q = cq)
    }

  def queryWithOptionalHeadTerms =
    ((("(" ~> rep1sep(annotatedHeadTerm, ",") <~ ")") | repsep(annotatedHeadTerm, ",")) // head terms with parentheses or optionally without
      ~ conjunctiveQueryBody("?-")) ^^ {
      case headTermsZippedWithAnnotations ~ cq =>
        val (headTerms, headTermAnnos) = headTermsZippedWithAnnotations unzip
        val headTermsToUse =
          if (headTerms nonEmpty) headTerms else {
            val definedVarsByBodies = cq.bodies map { _ flatMap DeepDiveLogSemanticChecker.collectDefinedVars }
            val definedVarsCommon = definedVarsByBodies reduce {_ intersect _}
            definedVarsCommon.toSet.toList map VarExpr
          }
        cq.copy(
          headTerms = headTermsToUse,
          headTermAnnotations = headTermAnnos,
          isForQuery = true
        )
    }

  def annotatedHeadTerm = rep(annotation) ~ expr ^^ { case anno ~ e => (e, anno) }





  def parseProgram(inputProgram: CharSequence, fileName: Option[String] = None): List[Statement] = {
    val result = parse(program, inputProgram)
    if (result successful) result.get
    else sys.error(fileName.getOrElse("") + result.toString)
  }

  def parseProgramFile(fileName: String): List[Statement] = {
    val source = scala.io.Source.fromFile(fileName)
    try parseProgram(source.getLines mkString "\n", Some(fileName))
    finally source.close()
  }

  def parseQuery(inputQuery: String): DeepDiveLog.Query = {
    val result = parse(phrase(query), inputQuery)
    if (result successful) result.get
    else sys.error(result.toString)
  }

}
