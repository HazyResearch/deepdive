package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object FactorWeightParser extends RegexParsers {
  def relationOrField = """[^,()]+""".r
  def weightVariable = relationOrField
  
  def constantWeight = """-?[\d\.]+""".r ^^ { x => KnownFactorWeight(x.toDouble) } 
  def unknownWeight = "?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }
  def factorWeight = constantWeight | unknownWeight

}