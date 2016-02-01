package org.deepdive.ddlog

// DeepDiveLog syntax
// See: https://docs.google.com/document/d/1SBIvvki3mnR28Mf0Pkin9w9mWNam5AA0SpIGj1ZN2c4

import scala.util.parsing.combinator._
import org.apache.commons.lang3.StringEscapeUtils
import scala.util.Try

// ***************************************
// * The union types for for the parser. *
// ***************************************
case class Variable(varName : String, relName : String, index : Int )

sealed trait Expr
case class VarExpr(name: String) extends Expr
sealed trait ConstExpr extends Expr
case class StringConst(value: String) extends ConstExpr
case class IntConst(value: Int) extends ConstExpr
case class DoubleConst(value: Double) extends ConstExpr
case class BooleanConst(value: Boolean) extends ConstExpr
case class NullConst extends ConstExpr
case class FuncExpr(function: String, args: List[Expr], isAggregation: Boolean) extends Expr
case class BinaryOpExpr(lhs: Expr, op: String, rhs: Expr) extends Expr
case class TypecastExpr(lhs: Expr, rhs: String) extends Expr
case class IfThenElseExpr(ifCondThenExprPairs: List[(Cond, Expr)], elseExpr: Option[Expr]) extends Expr

sealed trait Pattern
case class VarPattern(name: String) extends Pattern
case class ExprPattern(expr: Expr) extends Pattern
case class PlaceholderPattern extends Pattern

sealed trait Body
case class BodyAtom(name : String, terms : List[Pattern]) extends Body
case class QuantifiedBody(modifier: BodyModifier, bodies: List[Body]) extends Body

sealed trait BodyModifier
case class ExistModifier(negated: Boolean) extends BodyModifier
case class OuterModifier extends BodyModifier
case class AllModifier extends BodyModifier

case class Attribute(name : String, terms : List[String], types : List[String], annotations : List[List[Annotation]])
case class ConjunctiveQuery(headTerms: List[Expr], bodies: List[List[Body]], isDistinct: Boolean = false, limit: Option[Int] = None,
                            // optional annotations for head terms
                            headTermAnnotations: List[List[Annotation]] = List.empty,
                            // XXX This flag is not ideal, but minimizes the impact of query treatment when compared to creating another case class
                            isForQuery: Boolean = false
                           )
case class Column(name : String // name of the column
                 , t : String // type of the column
                 , annotation: List[Annotation] = List.empty // optional annotation
                 )

case class Annotation( name : String // name of the annotation
                     , args : Option[Either[Map[String, Any],List[Any]]] = None // optional, named arguments map or unnamed list
                     )
case class RuleAnnotation(name: String, args: List[String])

// condition
sealed trait Cond extends Body
case class ExprCond(expr: Expr) extends Cond
case class NegationCond(cond: Cond) extends Cond
case class CompoundCond(lhs: Cond, op: LogicOperator.LogicOperator, rhs: Cond) extends Cond

// logic operators
object LogicOperator extends Enumeration {
  type LogicOperator = Value
  val  AND, OR = Value
}

// variable type
sealed trait VariableType {
  def cardinality: Long
}
case object BooleanType extends VariableType {
  def cardinality = 2
}
case class MultinomialType(numCategories: Int) extends VariableType {
  def cardinality = numCategories
}

case class FactorWeight(variables: List[Expr])

// factor function
object FactorFunction extends Enumeration {
  type FactorFunction = Value
  val  IsTrue, Imply, Or, And, Equal, Multinomial, Linear, Ratio = Value
}
case class HeadAtom(name : String, terms : List[Expr])
case class InferenceRuleHead(function: FactorFunction.FactorFunction, terms: List[HeadAtom])


trait FunctionInputOutputType
case class RelationTypeDeclaration(names: List[String], types: List[String]) extends FunctionInputOutputType
case class RelationTypeAlias(likeRelationName: String) extends FunctionInputOutputType

trait FunctionImplementationDeclaration
case class RowWiseLineHandler(style: String, command: String) extends FunctionImplementationDeclaration

// Statements that will be parsed and compiled
trait Statement
case class SchemaDeclaration( a : Attribute
                            , isQuery : Boolean
                            , variableType : Option[VariableType]
                            , annotation : List[Annotation] = List.empty // optional annotation
                            ) extends Statement // atom and whether this is a query relation.
case class FunctionDeclaration( functionName: String, inputType: FunctionInputOutputType,
  outputType: FunctionInputOutputType, implementations: List[FunctionImplementationDeclaration]) extends Statement
case class ExtractionRule(headName: String, q : ConjunctiveQuery, supervision: Option[Expr] = None) extends Statement // Extraction rule
case class FunctionCallRule(output: String, function: String, q : ConjunctiveQuery, mode: Option[String], parallelism: Option[Int]) extends Statement // Extraction rule
case class InferenceRule(head: InferenceRuleHead, q : ConjunctiveQuery, weights : FactorWeight, mode: Option[String] = None) extends Statement // Weighted rule

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
  def relationName = ident
  def columnName   = ident
  def columnType   = ident ~ ("[]"?) ^^ {
      case ty ~ isArrayType => ty + isArrayType.getOrElse("")
    }
  def variableName = ident
  def functionName = ident
  def functionModeType = stringLiteralAsString
  def inferenceModeType = stringLiteralAsString
  def annotationName = ident
  def annotationArgumentName = ident

  def annotation: Parser[Annotation] =
    "@" ~ annotationName ~ opt(annotationArguments) ^^ {
      case (_ ~ name ~ optArgs) => Annotation(name, optArgs)
    }

  def annotationArguments: Parser[Either[Map[String, Any], List[Any]]] =
    "(" ~> (
      // XXX rep1sep must be above repsep to parse "()"
      rep1sep(annotationArgumentValue, ",") ^^ {
        case values => Right(values)
      }
    |
      repsep(annotationNamedArgument, ",") ^^ {
        case namedArgs => Left(namedArgs toMap)
      }
    ) <~ ")"
  def annotationNamedArgument: Parser[(String, Any)] =
    annotationArgumentName ~ "=" ~ annotationArgumentValue ^^ {
      case name ~ _ ~ value => name -> value
    }
  def annotationArgumentValue: Parser[Any] =
    stringLiteralAsString | double | integer

  def columnDeclaration: Parser[Column] =
    rep(annotation) ~
    columnName ~ columnType ^^ {
      case(anno ~ name ~ ty) => Column(name, ty, anno)
    }

  def CategoricalParser = "Categorical" ~> "(" ~> """\d+""".r <~ ")" ^^ { n => MultinomialType(n.toInt) }
  def BooleanParser = "Boolean" ^^ { s => BooleanType }
  def dataType = CategoricalParser | BooleanParser

  def schemaDeclaration: Parser[SchemaDeclaration] =
    rep(annotation) ~
    relationName ~ opt("?") ~ "(" ~ rep1sep(columnDeclaration, ",") ~ ")" ~ opt(dataType) ^^ {
      case (anno ~ r ~ isQuery ~ "(" ~ attrs ~ ")" ~ vType) => {
        val vars = attrs map (_.name)
        var types = attrs map (_.t)
        val annos = attrs map (_.annotation)
        val variableType = vType match {
          case None => if (isQuery != None) Some(BooleanType) else None
          case Some(s) => Some(s)
        }
        SchemaDeclaration(Attribute(r, vars, types, annos), (isQuery != None), variableType, anno)
      }
    }

  def operator =
    ( "||" | "+" | "-" | "*" | "/" | "&" | "%" )
  def typeOperator = "::"
  val aggregationFunctions = Set("MAX", "SUM", "MIN", "ARRAY_ACCUM", "ARRAY_AGG", "COUNT")

  // expression
  def expr : Parser[Expr] =
    ( lexpr ~ operator ~ expr ^^ { case (lhs ~ op ~ rhs) => BinaryOpExpr(lhs, op, rhs) }
    | lexpr ~ typeOperator ~ columnType ^^ { case (lhs ~ _ ~ rhs) => TypecastExpr(lhs, rhs) }
    | lexpr
    )

  def cexpr =
    ( expr ~ compareOperator ~ expr ^^ { case (lhs ~ op ~ rhs) => BinaryOpExpr(lhs, op, rhs) }
    | expr
    )

  def lexpr =
    ( "if" ~> (cond ~ ("then" ~> expr) ~ rep(elseIfExprs) ~ opt("else" ~> expr)) <~ "end" ^^ {
        case (ifCond ~ thenExpr ~ elseIfs ~ optElseExpr) =>
          IfThenElseExpr((ifCond, thenExpr) :: elseIfs, optElseExpr)
      }
    | stringLiteralAsString ^^ { StringConst(_) }
    | double ^^ { DoubleConst(_) }
    | integer ^^ { IntConst(_) }
    | ("TRUE" | "FALSE") ^^ { x => BooleanConst(x.toBoolean) }
    | "NULL" ^^ { _ => new NullConst }
    | functionName ~ "(" ~ rep1sep(expr, ",") ~ ")" ^^ {
        case (name ~ _ ~ args ~ _) => FuncExpr(name, args, (aggregationFunctions contains name))
      }
    | variableName ^^ { VarExpr(_) }
    | "(" ~> expr <~ ")"
    )

  def elseIfExprs =
    ("else" ~> "if" ~> cond) ~ ("then" ~> expr) ^^ {
      case (ifCond ~ thenExpr) => (ifCond, thenExpr)
    }

  // conditional expressions
  def compareOperator = "LIKE" | ">" | "<" | ">=" | "<=" | "!=" | "=" | "IS" ~ "NOT" ^^ { _ => "IS NOT" } | "IS"

  def cond : Parser[Cond] =
    ( acond ~ (";") ~ cond ^^ { case (lhs ~ op ~ rhs) =>
        CompoundCond(lhs, LogicOperator.OR, rhs)
      }
    | acond
    )
  def acond : Parser[Cond] =
    ( lcond ~ (",") ~ acond ^^ { case (lhs ~ op ~ rhs) => CompoundCond(lhs, LogicOperator.AND, rhs) }
    | lcond
    )
  // ! has higher priority...
  def lcond : Parser[Cond] =
    ( "!" ~> bcond ^^ { NegationCond(_) }
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
    )

  def atom = relationName ~ "(" ~ rep1sep(pattern, ",") ~ ")" ^^ {
    case (r ~ _ ~ patterns ~ _) => BodyAtom(r, patterns)
  }
  def quantifiedBody = (opt("!") ~ "EXISTS" | "OPTIONAL" | "ALL") ~ "[" ~ rep1sep(cqBody, ",") ~ "]" ^^ { case (m ~ _ ~ b ~ _) =>
    val modifier = m match {
      case (not ~ "EXISTS") => new ExistModifier(not != None)
      case "OPTIONAL" => new OuterModifier
      case "ALL" => new AllModifier
    }
    QuantifiedBody(modifier, b)
  }

  def functionInputOutputType: Parser[FunctionInputOutputType] =
    ( "rows" ~> "like" ~> relationName ^^ { RelationTypeAlias(_) }
    | "(" ~> rep1sep(columnDeclaration, ",") <~ ")" ^^ {
        attrs => RelationTypeDeclaration(attrs map { _.name }, attrs map { _.t })
      }
    )

  def ruleAnnotation = "@" ~> annotationName ~ ("(" ~> rep1sep(ident, ",") <~ ")") ^^ {
    case (name ~ args) => RuleAnnotation(name, args)
  }

  def ruleAnnotations = rep(ruleAnnotation)

  def getArg(annos: List[RuleAnnotation], name: String)  = {
    annos.find(_.name == name) map (_.args) map (_(0))
  }

  def functionImplementation : Parser[FunctionImplementationDeclaration] =
    ( "implementation" ~ stringLiteralAsString ~ "handles" ~ ("tsv" | "json") ~ "lines" ^^ {
        case (_ ~ command ~ _ ~ style ~ _) => RowWiseLineHandler(command=command, style=style)
      }
    | "implementation" ~ stringLiteralAsString ~ "runs" ~ "as" ~ "plpy" ^^ {
        case (_ ~ command ~ _ ~ _ ~ style) => RowWiseLineHandler(command=command, style=style)
      }
    )

  def functionDeclaration : Parser[FunctionDeclaration] =
    ( "function" ~ functionName ~ "over" ~ functionInputOutputType
                             ~ "returns" ~ functionInputOutputType
                 ~ (functionImplementation+)
    ) ^^ {
      case ("function" ~ a ~ "over" ~ inTy
                           ~ "returns" ~ outTy
                       ~ implementationDecls) =>
             FunctionDeclaration(a, inTy, outTy, implementationDecls)
    }

  def cqBody: Parser[Body] = quantifiedBody | atom | cond

  def cqConjunctiveBody: Parser[List[Body]] = rep1sep(cqBody, ",")

  def cqHeadTerms = "(" ~> rep1sep(expr, ",") <~ ")"

  def conjunctiveQueryBody : Parser[ConjunctiveQuery] =
    opt("*") ~ opt("|" ~> decimalNumber) ~ ":-" ~ rep1sep(cqConjunctiveBody, ";") ^^ {
      case (isDistinct ~ limit ~ ":-" ~ disjunctiveBodies) =>
        ConjunctiveQuery(List.empty, disjunctiveBodies, isDistinct != None, limit map (_.toInt))
    }

  def conjunctiveQuery : Parser[ConjunctiveQuery] =
    // TODO fill headTermAnnotations as done in queryWithOptionalHeadTerms to support @order_by
    cqHeadTerms ~ conjunctiveQueryBody ^^ {
      case (head ~ cq) => cq.copy(headTerms = head)
    }

  def functionMode = "@mode" ~> commit("(" ~> functionModeType <~ ")" ^? ({
    case "inc" => "inc"
  }, (s) => s"${s}: unrecognized mode"))

  def parallelism = "@parallelism" ~> "(" ~> integer <~ ")"

  def functionCallRule : Parser[FunctionCallRule] =
    opt(functionMode) ~ opt(parallelism) ~ relationName ~ "+=" ~ functionName ~ conjunctiveQuery ^^ {
      case (mode ~ parallelism ~ out ~ _ ~ func ~ cq) => FunctionCallRule(out, func, cq, mode, parallelism)
    }

  def supervisionAnnotation = "@label" ~> "(" ~> expr <~ ")"

  def conjunctiveQueryWithSupervision : Parser[ConjunctiveQuery] =
    cqHeadTerms ~ opt("*") ~ opt("|" ~> decimalNumber) ~ ":-" ~ rep1sep(cqConjunctiveBody, ";") ^^ {
      case (head ~ isDistinct ~ limit ~ ":-" ~ disjunctiveBodies) =>
        ConjunctiveQuery(head, disjunctiveBodies, isDistinct != None, limit map (_.toInt))
  }

  def extractionRule =
    ( opt(supervisionAnnotation) ~ relationName ~ conjunctiveQuery ^^ {
        case (sup ~ head ~ cq) => ExtractionRule(head, cq, sup)
      }
    | relationName ~ cqHeadTerms ~ ("=" ~> expr) ~ conjunctiveQueryBody ^^ {
        case (head ~ headTerms ~ sup ~ cq) =>
          ExtractionRule(head, cq.copy(headTerms = headTerms), Some(sup))
      }
    )

  def factorWeight = "@weight" ~> "(" ~> rep1sep(expr, ",") <~ ")" ^^ { FactorWeight(_) }
  def inferenceMode = "@mode" ~> commit("(" ~> inferenceModeType <~ ")" ^? ({
    case "inc" => "inc"
  }, (s) => s"${s}: unrecognized mode"))

  // factor functions
  def headAtom = relationName ~ ("(" ~> rep1sep(expr, ",") <~ ")") ^^ {
    case (r ~ expressions) => HeadAtom(r, expressions)
  }

  def implyHeadAtoms = rep1sep(headAtom, ",") ~ "=>" ~ headAtom ^^ {
    case (a ~ _ ~ b) => a :+ b
  }

  def inferenceRuleHead =
  ( implyHeadAtoms ^^ {
      InferenceRuleHead(FactorFunction.Imply, _)
    }
  | "@semantics" ~> commit("(" ~> stringLiteralAsString <~ ")" ^? ({
      case "linear" => FactorFunction.Linear
      case "ratio"  => FactorFunction.Ratio
    }, (s) => s"${s}: unrecognized semantics")) ~ implyHeadAtoms ^^ {
      case (s ~ h) => InferenceRuleHead(s, h)
    }
  | headAtom ~ "=" ~ rep1sep(headAtom, "=") ^^ { case (a ~ _ ~ b) =>
      InferenceRuleHead(FactorFunction.Equal, a +: b)
    }
  | headAtom ~ "^" ~ rep1sep(headAtom, "^") ^^ { case (a ~ _ ~ b) =>
      InferenceRuleHead(FactorFunction.And, a +: b)
    }
  | headAtom ~ "v" ~ rep1sep(headAtom, "v") ^^ { case (a ~ _ ~ b) =>
      InferenceRuleHead(FactorFunction.Or, a +: b)
    }
  | "Multinomial" ~> "(" ~> rep1sep(headAtom, ",") <~ ")" ^^ {
      InferenceRuleHead(FactorFunction.Multinomial, _)
    }
  | headAtom ^^ {
      x => InferenceRuleHead(FactorFunction.IsTrue, List(x))
    }
  )

  def inferenceConjunctiveQuery : Parser[ConjunctiveQuery] =
    ":-" ~> rep1sep(cqConjunctiveBody, ";") ^^ {
      ConjunctiveQuery(List(), _, false, None)
  }

  def inferenceRule =
    opt(inferenceMode) ~ factorWeight ~ inferenceRuleHead ~ inferenceConjunctiveQuery ^^ {
      case (mode ~ weight ~ head ~ cq) => InferenceRule(head, cq, weight, mode)
  }

  // rules or schema elements in arbitrary order
  def statement : Parser[Statement] = ( inferenceRule
                                      | schemaDeclaration
                                      | extractionRule
                                      | functionDeclaration
                                      | functionCallRule
                                      )
  def program : Parser[DeepDiveLog.Program] = phrase(rep1(statement <~ "."))

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
      ~ opt("*") ~ opt("|" ~> decimalNumber)) ~ ("?-" ~> cqConjunctiveBody) ^^ {
      case (headTermsZippedWithAnnotations ~ isDistinct ~ limit) ~ body =>
        val (headTerms, headTermAnnos) = headTermsZippedWithAnnotations unzip
        val headTermsToUse =
          if (headTerms nonEmpty) headTerms else {
            val definedVars = body flatMap DeepDiveLogSemanticChecker.collectDefinedVars
            definedVars map VarExpr
          }
        ConjunctiveQuery(
          headTerms = headTermsToUse,
          headTermAnnotations = headTermAnnos,
          bodies = List(body),
          isDistinct = isDistinct != None,
          limit = limit map (_.toInt),
          isForQuery = true
        )
    }

  def annotatedHeadTerm = rep(annotation) ~ expr ^^ { case anno ~ e => (e, anno) }

  def parseQuery(inputQuery: String): DeepDiveLog.Query = {
    val result = parse(phrase(query), inputQuery)
    if (result successful) result.get
    else sys.error(result.toString)
  }

}
