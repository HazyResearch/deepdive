package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.context._


object FactorWeightParser extends RegexParsers {
  def relationOrField = """\w+""".r
  def weightVariable = (relationOrField <~ ".") ~ (relationOrField) ^^ { 
    case relationName ~ field => s"${relationName}.${field}"
  }
  def constantWeight = """-?\d+""".r ^^ { x => KnownFactorWeight(x.toDouble) } 
  def unknownWeight = "?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }
  def factorWeight = constantWeight | unknownWeight

}