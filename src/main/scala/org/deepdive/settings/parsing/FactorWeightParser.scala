package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object FactorWeightParser extends RegexParsers {
  def relationOrField = """\w+""".r
  def weightVariable = rep1sep(relationOrField, ".") ^^ { 
    case fields => fields.mkString(".")
  }
  def constantWeight = """-?[\d\.]+""".r ^^ { x => KnownFactorWeight(x.toDouble) } 
  def unknownWeight = "?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }
  def factorWeight = constantWeight | unknownWeight

}