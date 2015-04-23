// DeepDiveLog syntax
// See: https://docs.google.com/document/d/1SBIvvki3mnR28Mf0Pkin9w9mWNam5AA0SpIGj1ZN2c4

import scala.util.parsing.combinator._
import org.apache.commons.lang3.StringEscapeUtils

// ***************************************
// * The union types for for the parser. *
// ***************************************
case class Variable(varName : String, relName : String, index : Int )
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

// Parser
class ConjunctiveQueryParser extends JavaTokenParsers {

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
  def columnDeclaration : Parser[Column] =
    columnName ~ columnType ^^ {
      case(name ~ ty) => Column(name, ty)
    }

  def atom: Parser[Atom] =
    relationName ~ "(" ~ rep1sep(variableName, ",") ~ ")" ^^ {
      case (r ~ "(" ~ cols ~ ")") =>
        Atom(r, cols.zipWithIndex map { case(name,i) => Variable(name, r, i) })
    }

  def relationDeclaration: Parser[Attribute] =
    relationName ~ "(" ~ rep1sep(columnDeclaration, ",") ~ ")" ^^ {
      case (r ~ "(" ~ attrs ~ ")") => {
        val vars = attrs.zipWithIndex map { case(x, i) => Variable(x.name, r, i) }
        var types = attrs map { case(x) => x.t }
        Attribute(r, vars, types)
      }
    }

  def query : Parser[ConjunctiveQuery] =
    atom ~ ":-" ~ rep1sep(atom, ",") ^^ {
      case (headatom ~ ":-" ~ bodyatoms) =>
        ConjunctiveQuery(headatom, bodyatoms.toList)
    }

  def schemaDeclaration : Parser[SchemaElement] =
    relationDeclaration ~ opt("?") ^^ {
      case (a ~ None   ) => SchemaElement(a,true)
      case (a ~ Some(_)) => SchemaElement(a,false)
    }


  def functionDeclaration : Parser[FunctionElement] =
    ( "function" ~ functionName
    ~ "over" ~ "like" ~ relationName
    ~ "returns" ~ "like" ~ relationName
    ~ "implementation" ~ stringLiteralAsString ~ "handles" ~ ("tsv" | "json") ~ "lines"
    ) ^^ {
      case ("function" ~ a
           ~ "over" ~ "like" ~ b
           ~ "returns" ~ "like" ~ c
           ~ "implementation" ~ d ~ "handles" ~ e ~ "lines") =>
             FunctionElement(a, b, c, d, e)
    }

  def extractionRule : Parser[ExtractionRule] =
    query ^^ {
      ExtractionRule(_)
    }

  def functionCallRule : Parser[FunctionRule] =
    ( relationName ~ ":-" ~ "!"
    ~ functionName ~ "(" ~ relationName ~ ")"
    ) ^^ {
      case (out ~ ":-" ~ "!" ~ fn ~ "(" ~ in ~ ")") =>
        FunctionRule(in, out, fn)
    }

  def constantWeight = floatingPointNumberAsDouble ^^ {   KnownFactorWeight(_) }
  def unknownWeight  = repsep(variableName, ",")   ^^ { UnknownFactorWeight(_) }
  def factorWeight = "weight" ~> "=" ~> (constantWeight | unknownWeight)

  def supervision = "label" ~> "=" ~> variableName

  def inferenceRule : Parser[InferenceRule] =
    ( query ~ factorWeight ~ supervision
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
  def program : Parser[List[Statement]] = phrase(rep1(statement <~ "."))

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
