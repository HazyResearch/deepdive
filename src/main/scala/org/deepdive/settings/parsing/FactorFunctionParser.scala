package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers

object FactorFunctionParser extends RegexParsers {
  def relationOrField = """\w+""".r
  
  def factorVariable = ((relationOrField <~ "->")?) ~ (relationOrField) ^^ { 
    case foreignKey ~ field => 
      FactorFunctionVariable(foreignKey, field)
  }

  def factorFunc = factorVariable ~ ("=" ~> "Imply" ~>  "(" ~> repsep(factorVariable, ",") <~ ")") ^^ { 
    case headVariable ~ varList =>
      ImplyFactorFunction(headVariable, varList)
  }

}