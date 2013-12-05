package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.context._


object FactorWeightParser extends RegexParsers {
  def weightVariable = """\w+""".r
  def constantWeight = """-?\d+""".r ^^ { x => KnownFactorWeight(x.toDouble) } 
  def unknownWeight = "?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }
  def factorWeight = constantWeight | unknownWeight

}