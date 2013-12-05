package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers

object FactorFunctionParser extends RegexParsers {
  def factorVariable = """\w+""".r
  def factorFunc = factorVariable ~ ("=" ~> "Imply" ~>  "(" ~> repsep(factorVariable, ",") <~ ")") ^^ { 
    case headVariable ~ varList =>
      ImplyFactorFunction(headVariable, varList)
  }

}