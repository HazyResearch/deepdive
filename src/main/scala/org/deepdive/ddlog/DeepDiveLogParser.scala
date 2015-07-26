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

sealed trait Pattern
case class VarPattern(name: String) extends Pattern
case class ExprPattern(expr: Expr) extends Pattern
case class PlaceholderPattern extends Pattern

case class HeadAtom(name : String, terms : List[Expr])

sealed trait Body
case class BodyAtom(name : String, terms : List[Pattern]) extends Body
case class QuantifiedBody(modifier: BodyModifier, bodies: List[Body]) extends Body

sealed trait BodyModifier
case class ExistModifier(negated: Boolean) extends BodyModifier
case class OuterModifier extends BodyModifier
case class AllModifier extends BodyModifier

case class Attribute(name : String, terms : List[String], types : List[String], annotations : List[List[Annotation]])
case class ConjunctiveQuery(head: HeadAtom, bodies: List[List[Body]], isDistinct: Boolean, limit: Option[Int])
case class Column(name : String // name of the column
                 , t : String // type of the column
                 , annotation: List[Annotation] = List.empty // optional annotation
                 )

case class Annotation( name : String // name of the annotation
                     , args : Map[String, Any] = Map.empty // optional, named arguments
                     )
case class RuleAnnotation(name: String, args: List[Any])

// condition
sealed trait Cond extends Body
case class ComparisonCond(lhs: Expr, op: String, rhs: Expr) extends Cond
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

trait RelationType
case class RelationTypeDeclaration(names: List[String], types: List[String]) extends RelationType
case class RelationTypeAlias(likeRelationName: String) extends RelationType

trait FunctionImplementationDeclaration
case class RowWiseLineHandler(style: String, command: String) extends FunctionImplementationDeclaration

// Statements that will be parsed and compiled
trait Statement
case class SchemaDeclaration( a : Attribute
                            , isQuery : Boolean
                            , variableType : Option[VariableType]
                            , annotation : List[Annotation] = List.empty // optional annotation
                            ) extends Statement // atom and whether this is a query relation.
case class FunctionDeclaration( functionName: String, inputType: RelationType, outputType: RelationType, implementations: List[FunctionImplementationDeclaration], mode: String = null) extends Statement
case class ExtractionRule(q : ConjunctiveQuery, supervision: Option[String] = None) extends Statement // Extraction rule
case class FunctionCallRule(input : String, output : String, function : String) extends Statement // Extraction rule
case class InferenceRule(q : ConjunctiveQuery, weights : FactorWeight, function : Option[String], mode: String = null) extends Statement // Weighted rule

// Parser
class DeepDiveLogParser extends JavaTokenParsers {

  // JavaTokenParsers provides several useful number parsers:
  //   wholeNumber, decimalNumber, floatingPointNumber
  def double = opt("-") ~ """\d+\.\d+""".r ^^ { case (neg ~ num) => (neg.getOrElse("") + num).toDouble }
  def integer = opt("-") ~ wholeNumber ^^ { case (neg ~ num) => (neg.getOrElse("") + num).toInt }
  def stringLiteralAsString = stringLiteral ^^ {
    s => StringEscapeUtils.unescapeJava(
      s.stripPrefix("\"").stripSuffix("\""))
  }

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
  def semanticType = ident
  def functionModeType = ident
  def inferenceModeType = ident
  def annotationName = ident
  def annotationArgumentName = ident

  def annotation: Parser[Annotation] =
    "@" ~ annotationName ~ opt("(" ~> repsep(annotationArgument, ",") <~ ")") ^^ {
      case (_ ~ name ~ optArgs) =>
        Annotation(name, optArgs getOrElse { List.empty } toMap)
    }

  def annotationArgument: Parser[(String, Any)] =
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

  def operator = "||" | "+" | "-" | "*" | "/" | "&"
  def typeOperator = "::"
  val aggregationFunctions = Set("MAX", "SUM", "MIN", "ARRAY_ACCUM", "ARRAY_AGG", "COUNT")

  // expression
  def expr : Parser[Expr] =
    ( lexpr ~ operator ~ expr ^^ { case (lhs ~ op ~ rhs) => BinaryOpExpr(lhs, op, rhs) }
    | lexpr ~ typeOperator ~ columnType ^^ { case (lhs ~ _ ~ rhs) => TypecastExpr(lhs, rhs) }
    | lexpr
    )

  def lexpr : Parser[Expr] =
    ( functionName ~ "(" ~ rep1sep(expr, ",") ~ ")" ^^ {
        case (name ~ _ ~ args ~ _) => FuncExpr(name, args, (aggregationFunctions contains name))
      }
    | stringLiteralAsString ^^ { StringConst(_) }
    | double ^^ { DoubleConst(_) }
    | integer ^^ { IntConst(_) }
    | ("TRUE" | "FALSE") ^^ { x => BooleanConst(x.toBoolean) }
    | "NULL" ^^ { _ => new NullConst }
    | variableName ^^ { VarExpr(_) }
    | "(" ~> expr <~ ")"
    )

  def cqHead = relationName ~ "(" ~ rep1sep(expr, ",") ~ ")" ^^ {
    case (r ~ _ ~ expressions ~ _) => HeadAtom(r, expressions)
  }

  // conditional expressions
  def compareOperator = "LIKE" | ">" | "<" | ">=" | "<=" | "!=" | "=" | "IS" | "IS NOT"

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
    ( expr ~ compareOperator ~ expr ^^ { case (lhs ~ op ~ rhs) =>
        ComparisonCond(lhs, op, rhs)
      }
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

  def cqBody: Parser[Body] = cond | quantifiedBody | atom

  def cqConjunctiveBody: Parser[List[Body]] = rep1sep(cqBody, ",")

  def conjunctiveQuery : Parser[ConjunctiveQuery] =
    cqHead ~ opt("*") ~ opt("|" ~> decimalNumber) ~ ":-" ~ rep1sep(cqConjunctiveBody, ";") ^^ {
      case (headatom ~ isDistinct ~ limit ~ ":-" ~ disjunctiveBodies) =>
        ConjunctiveQuery(headatom, disjunctiveBodies, isDistinct != None, limit map (_.toInt))
  }

  def relationType: Parser[RelationType] =
    ( "like" ~> relationName ^^ { RelationTypeAlias(_) }
    | rep1sep(columnDeclaration, ",") ^^ {
        attrs => RelationTypeDeclaration(attrs map { _.name }, attrs map { _.t })
      }
    )

  def ruleAnnotation(arg: Parser[Any]) = "@" ~> annotationName ~ "(" ~ rep1sep(arg, ",") <~ ")" ^^ {
    case (name ~ _ ~ args) => RuleAnnotation(name, args)
  }

  def ruleAnnotations(arg: Parser[Any]) = rep(ruleAnnotation(arg))

  def functionMode = "@mode" ~> "(" ~> functionModeType <~ ")"
  def inferenceMode = "@mode" ~> "(" ~> inferenceModeType <~ ")"

  def functionImplementation : Parser[FunctionImplementationDeclaration] =
    ( "implementation" ~ stringLiteralAsString ~ "handles" ~ ("tsv" | "json") ~ "lines" ^^ {
        case (_ ~ command ~ _ ~ style ~ _) => RowWiseLineHandler(command=command, style=style)
      }
    | "implementation" ~ stringLiteralAsString ~ "runs" ~ "as" ~ "plpy" ^^ {
        case (_ ~ command ~ _ ~ _ ~ style) => RowWiseLineHandler(command=command, style=style)
      }
    )

  def functionDeclaration : Parser[FunctionDeclaration] =
    ( opt(functionMode) ~ "function" ~ functionName ~ "over" ~ relationType
                             ~ "returns" ~ relationType
                 ~ (functionImplementation+)
    ) ^^ {
      case (mode ~ "function" ~ a ~ "over" ~ inTy
                           ~ "returns" ~ outTy
                       ~ implementationDecls) =>
             FunctionDeclaration(a, inTy, outTy, implementationDecls, mode.getOrElse(null))
    }

  def extractionRule : Parser[ExtractionRule] =
    opt(supervision) ~ conjunctiveQuery ^^ {
      case (supervision ~ q) => ExtractionRule(q, supervision)
    }

  def functionCallRule : Parser[FunctionCallRule] =
    ( relationName ~ ":-" ~ "!" ~ functionName ~ "(" ~ relationName ~ ")" ) ^^ {
      case (out ~ ":-" ~ _ ~ fn ~ "(" ~ in ~ ")") =>
        FunctionCallRule(in, out, fn)
    }

  def factorWeight = "@weight" ~> "(" ~> rep1sep(expr, ",") <~ ")" ^^ { FactorWeight(_) }

  def supervision = "@label" ~> "(" ~> variableName <~ ")"

  def factorFunctionName = "Imply" | "And" | "Equal" | "Or" | "Multinomial" | "Linear" | "Ratio"
  def factorFunction = "@function" ~> "(" ~> factorFunctionName <~ ")"

  def inferenceRule : Parser[InferenceRule] =
    ( opt(inferenceMode) ~ opt(factorFunction) ~ factorWeight ~ conjunctiveQuery
    ) ^^ {
      case (mode ~ function ~ weight ~ q) =>
        InferenceRule(q, weight, function, mode.getOrElse(null))
    }

  // rules or schema elements in arbitrary order
  def statement : Parser[Statement] = ( schemaDeclaration
                                      | inferenceRule
                                      | extractionRule
                                      | functionDeclaration
                                      | functionCallRule
                                      )
  def program : Parser[DeepDiveLog.Program] = phrase(rep1(statement <~ "."))

  def parseProgram(inputProgram: CharSequence, fileName: Option[String] = None): List[Statement] = {
    parse(program, inputProgram) match {
      case result: Success[_] => result.get
      case error:  NoSuccess  => throw new RuntimeException(fileName.getOrElse("") + error.toString())
    }
  }

  def parseProgramFile(fileName: String): List[Statement] = {
    val source = scala.io.Source.fromFile(fileName)
    try parseProgram(source.getLines mkString "\n", Some(fileName))
    finally source.close()
  }
}
