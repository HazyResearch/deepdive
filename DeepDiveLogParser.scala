// DeepDiveLog syntax

import scala.util.parsing.combinator._

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
  // Odd definitions, but we'll keep them.
  // def stringliteral1: Parser[String] = ("'"+"""([^'\p{Cntrl}\\]|\\[\\"'bfnrt]|\\u[a-fA-F0-9]{4})*"""+"'").r ^^ {case (x) => x}
  // def stringliteral2: Parser[String] = """[a-zA-Z_0-9\./]*""".r ^^ {case (x) => x}
  // def stringliteral: Parser[String] = (stringliteral1 | stringliteral2) ^^ {case (x) => x}
  def stringliteral: Parser[String] = """[a-zA-Z0-9_\[\]]+""".r
  def path: Parser[String] = """[a-zA-Z0-9\./_]+""".r

  // relation names and columns are just strings.
  def relation_name: Parser[String] = stringliteral ^^ {case (x) => x}
  def col : Parser[String] = stringliteral  ^^ { case(x) => x }
  def attr : Parser[Column] = stringliteral ~ stringliteral ^^ {
    case(x ~ y) => Column(x, y)
  }

  def atom: Parser[Atom] = relation_name ~ "(" ~ rep1sep(col, ",") ~ ")" ^^ {
    case (r ~ "(" ~ cols ~ ")") => {
      val vars = cols.zipWithIndex map { case(name,i) => Variable(name, r, i) }
      Atom(r,vars)
    }
  }

  def attribute: Parser[Attribute] = relation_name ~ "(" ~ rep1sep(attr, ",") ~ ")" ^^ {
    case (r ~ "(" ~ attrs ~ ")") => {
      val vars = attrs.zipWithIndex map { case(x, i) => Variable(x.name, r, i) }
      var types = attrs map { case(x) => x.t }
      Attribute(r,vars, types)
    }
  }

  def udf : Parser[String] = stringliteral ^^ {case (x) => x}

  def query : Parser[ConjunctiveQuery] = atom ~ ":-" ~ rep1sep(atom, ",") ^^ {
    case (headatom ~ ":-" ~ bodyatoms) => ConjunctiveQuery(headatom, bodyatoms.toList)
  }

  def schemaElement : Parser[SchemaElement] = attribute ~ opt("?") ^^ {
    case (a ~ None) => SchemaElement(a,true)
    case (a ~ Some(_)) =>  SchemaElement(a,false)
  }


  def functionElement : Parser[FunctionElement] = "function" ~ stringliteral ~
  "over like" ~ stringliteral ~ "returns like" ~ stringliteral ~ "implementation" ~
  "\"" ~ path ~ "\"" ~ "handles" ~ stringliteral ~ "lines" ^^ {
    case ("function" ~ a ~ "over like" ~ b ~ "returns like" ~ c ~ "implementation" ~
      "\"" ~ d ~ "\"" ~ "handles" ~ e ~ "lines") => FunctionElement(a, b, c, d, e)
  }

  def extractionRule : Parser[ExtractionRule] = query  ^^ {
    case (q) => ExtractionRule(q)
    // case (q ~ "udf" ~ "=" ~ None)       => ExtractionRule(q,None)
  }

  def functionRule : Parser[FunctionRule] = stringliteral ~ ":-" ~ "!" ~ stringliteral ~ "(" ~ stringliteral ~ ")" ^^ {
    case (a ~ ":-" ~ "!" ~ b ~ "(" ~ c ~ ")") => FunctionRule(c, a, b)
  }

  def constantWeight = "weight" ~> "=" ~> """-?[\d\.]+""".r ^^ { x => KnownFactorWeight(x.toDouble) }
  def unknwonWeight = "weight" ~> "=" ~> opt(rep1sep(col, ",")) ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }
  def factorWeight = constantWeight | unknwonWeight

  def supervision = "label" ~> "=" ~> col

  def inferenceRule : Parser[InferenceRule] = query ~ factorWeight ~ supervision ^^ {
    case (q ~ weight ~ supervision) => InferenceRule(q, weight, supervision)
  }

  // rules or schema elements in aribitrary order
  def statement : Parser[Statement] = (functionElement | inferenceRule | extractionRule | functionRule | schemaElement) ^^ {case(x) => x}

  def program : Parser[List[Statement]] = rep1sep(statement, ".") ^^ { case(x) => x }

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
