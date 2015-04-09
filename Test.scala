import scala.util.parsing.combinator._
import scala.collection.immutable.HashMap

trait Statement
case class Variable(varName : String, relName: String, index : Int ) 
case class Atom(name : String, terms : List[Variable]) 
case class ConjunctiveQuery(head: Atom, body: List[Atom])

case class WeightedRule(q : ConjunctiveQuery, weights : Option[List[String]]) extends Statement // Weighted rule
case class SchemaElement( a : Atom , query : Boolean ) extends Statement // atom and whether this is a query relation.


// Parser
class ConjunctiveQueryParser extends JavaTokenParsers {   
  def stringliteral: Parser[String] = """[\w]+""".r

  // relation names and columns are just strings.
  def relation_name: Parser[String] = stringliteral ^^ {case (x) => x}
  def col : Parser[String] = stringliteral  ^^ { case(x) => x }

  def atom: Parser[Atom] = relation_name ~ "(" ~ rep1sep(col, ",") ~ ")" ^^ {
    case (r ~ "(" ~ cols ~ ")") => {
      val vars = cols.zipWithIndex map { case(name,i) => Variable(name, r, i) }
      Atom(r,vars)
    }
  }

  def query : Parser[ConjunctiveQuery] = atom ~ ":-" ~ rep1sep(atom, ",") ^^ {
    case (headatom ~ ":-" ~ bodyatoms) => ConjunctiveQuery(headatom, bodyatoms.toList)
  }

  def schema_element : Parser[SchemaElement] = atom ~ opt("!") ^^ {
    case (a ~ None) => SchemaElement(a,true)
    case (a ~ Some(_)) =>  SchemaElement(a,false)
  }


  def rule : Parser[WeightedRule] = query ~ opt( "weight=" ~ rep1sep(col, ",")) ^^ {
    case (q ~ Some("weight=" ~ weights)) => WeightedRule(q,Some(weights))
    case (q ~ None)                      => WeightedRule(q,None)
  }

  // rules or schema elements in aribitrary order
  def statement : Parser[Statement] = (rule | schema_element) ^^ {case(x) => x}

  def statements : Parser[List[Statement]] = rep1sep(statement, ";") ^^ { case(x) => x }
}

object Test extends ConjunctiveQueryParser  {

  def main(args: Array[String]) = {
    val q = parse(statements, args(0))
    println(q.get)
  }
}
