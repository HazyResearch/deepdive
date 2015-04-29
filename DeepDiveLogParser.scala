// DeepDiveLog syntax
// See: https://docs.google.com/document/d/1SBIvvki3mnR28Mf0Pkin9w9mWNam5AA0SpIGj1ZN2c4

import scala.util.parsing.combinator._
import org.apache.commons.lang3.StringEscapeUtils

// ***************************************
// * The union types for for the parser. *
// ***************************************
case class Variable(varName : String, relName : String, index : Int )
// TODO make Atom a trait, and have multiple case classes, e.g., RelationAtom and CondExprAtom
case class Atom(name : String, terms : List[Variable])
case class Attribute(name : String, terms : List[Variable], types : List[String])
case class ConjunctiveQuery(head: Atom, body: List[Atom])
case class Column(name : String, t : String)

sealed trait FactorWeight {
  def variables : List[String]
}

case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
}
case class UnknownFactorWeight(variables: List[String]) extends FactorWeight

trait RelationType
case class RelationTypeDeclaration(names: List[String], types: List[String]) extends RelationType
case class RelationTypeAlias(likeRelationName: String) extends RelationType

trait FunctionImplementationDeclaration
case class RowWiseLineHandler(format: String, command: String) extends FunctionImplementationDeclaration

// Statements that will be parsed and compiled
trait Statement
case class SchemaDeclaration( a : Attribute , isQuery : Boolean ) extends Statement // atom and whether this is a query relation.
case class FunctionDeclaration( functionName: String, inputType: RelationType, outputType: RelationType, implementations: List[FunctionImplementationDeclaration]) extends Statement
case class ExtractionRule(q : ConjunctiveQuery) extends Statement // Extraction rule
case class FunctionCallRule(input : String, output : String, function : String) extends Statement // Extraction rule
case class InferenceRule(q : ConjunctiveQuery, weights : FactorWeight, supervision : String) extends Statement // Weighted rule


// Parser
class DeepDiveLogParser extends JavaTokenParsers {

  // JavaTokenParsers provides several useful number parsers:
  //   wholeNumber, decimalNumber, floatingPointNumber 
  def floatingPointNumberAsDouble = floatingPointNumber ^^ { _.toDouble }
  def stringLiteralAsString = stringLiteral ^^ {
    s => StringEscapeUtils.unescapeJava(
      s.stripPrefix("\"").stripSuffix("\""))
  }

  // We just use Java identifiers to parse various names
  def relationName = ident
  def columnName   = ident
  def columnType   = ident ~ ("[]"?) ^^ {
      case ty ~ isArrayType => ty + isArrayType.getOrElse("")
    }
  def variableName = ident
  def functionName = ident

  def columnDeclaration: Parser[Column] =
    columnName ~ columnType ^^ {
      case(name ~ ty) => Column(name, ty)
    }
  def schemaDeclaration: Parser[SchemaDeclaration] =
    relationName ~ opt("?") ~ "(" ~ rep1sep(columnDeclaration, ",") ~ ")" ^^ {
      case (r ~ isQuery ~ "(" ~ attrs ~ ")") => {
        val vars = attrs.zipWithIndex map { case(x, i) => Variable(x.name, r, i) }
        var types = attrs map { case(x) => x.t }
        SchemaDeclaration(Attribute(r, vars, types), (isQuery != None))
      }
    }


  // TODO support aggregate function syntax somehow
  def cqHead = relationName ~ "(" ~ repsep(variableName, ",") ~ ")" ^^ {
      case (r ~ "(" ~ variableUses ~ ")") =>
        Atom(r, variableUses.zipWithIndex map {
          case(name,i) => Variable(name, r, i)
        })
    }

  // TODO add conditional expressions for where clause
  def cqConditionalExpr = failure("No conditional expression supported yet")
  def cqBodyAtom: Parser[Atom] =
    ( relationName ~ "(" ~ repsep(variableName, ",") ~ ")" ^^ {
        case (r ~ "(" ~ variableBindings ~ ")") =>
          Atom(r, variableBindings.zipWithIndex map {
            case(name,i) => Variable(name, r, i)
          })
      }
    | cqConditionalExpr
    )
  def cqBody: Parser[List[Atom]] = rep1sep(cqBodyAtom, ",")
  def conjunctiveQuery : Parser[ConjunctiveQuery] =
    cqHead ~ ":-" ~ rep1sep(cqBody, ";") ^^ {
      case (headatom ~ ":-" ~ disjunctiveBodies) =>
        // TODO handle all disjunctiveBodies
        // XXX only compiling the first body
        val bodyatoms = disjunctiveBodies(0)
        ConjunctiveQuery(headatom, bodyatoms.toList)
    }

  def relationType: Parser[RelationType] =
    ( rep1sep(columnDeclaration, ",") ^^ {
        attrs => RelationTypeDeclaration(attrs map { _.name }, attrs map { _.t })
      }
    | "like" ~> relationName ^^ { RelationTypeAlias(_) }
    )

  def functionImplementation : Parser[FunctionImplementationDeclaration] =
    "implementation" ~ stringLiteralAsString ~ "handles" ~ ("tsv" | "json") ~ "lines" ^^ {
      case (_ ~ command ~ _ ~ format ~ _) => RowWiseLineHandler(command=command, format=format)
    }

  def functionDeclaration : Parser[FunctionDeclaration] =
    ( "function" ~ functionName ~ "over" ~ relationType
                             ~ "returns" ~ relationType
                 ~ (functionImplementation+)
    ) ^^ {
      case ("function" ~ a ~ "over" ~ inTy
                           ~ "returns" ~ outTy
                       ~ implementationDecls) =>
             FunctionDeclaration(a, inTy, outTy, implementationDecls)
    }

  def extractionRule : Parser[ExtractionRule] =
    conjunctiveQuery ^^ {
      ExtractionRule(_)
    }

  def functionCallRule : Parser[FunctionCallRule] =
    ( relationName ~ ":-" ~ "!"
    ~ functionName ~ "(" ~ relationName ~ ")"
    ) ^^ {
      case (out ~ ":-" ~ "!" ~ fn ~ "(" ~ in ~ ")") =>
        FunctionCallRule(in, out, fn)
    }

  def constantWeight = floatingPointNumberAsDouble ^^ {   KnownFactorWeight(_) }
  def unknownWeight  = repsep(variableName, ",")   ^^ { UnknownFactorWeight(_) }
  def factorWeight = "weight" ~> "=" ~> (constantWeight | unknownWeight)

  def supervision = "label" ~> "=" ~> variableName

  def inferenceRule : Parser[InferenceRule] =
    ( conjunctiveQuery ~ factorWeight ~ supervision
    ) ^^ {
      case (q ~ weight ~ supervision) =>
        InferenceRule(q, weight, supervision)
    }

  // rules or schema elements in aribitrary order
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
