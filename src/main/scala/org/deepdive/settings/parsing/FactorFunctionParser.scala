package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers

object FactorFunctionParser extends RegexParsers {
  def relationOrField = """[\w]+""".r
  
  def factorVariable = rep1sep(relationOrField, ".") ^^ { 
    case varList => 
      FactorFunctionVariable(varList.take(varList.size - 1).mkString("."), varList.last)
  }

  def factorFunc = factorVariable ~ ("=" ~> "Imply" ~>  "(" ~> repsep(factorVariable, ",") <~ ")") ^^ { 
    case headVariable ~ varList =>
      ImplyFactorFunction(headVariable, varList)
  }

}