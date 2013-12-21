package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers

object FactorFunctionParser extends RegexParsers {
  def relationOrField = """[\w]+""".r
  def arrayDefinition = """\[\]""".r
  
  def factorVariable = rep1sep(relationOrField, ".") ~ (arrayDefinition?) ^^ { 
    case (varList ~ isArray)  => 
      FactorFunctionVariable(varList.take(varList.size - 1).mkString("."), varList.last, isArray.isDefined)
  }

  def factorFunc = factorVariable ~ ("=" ~> "Imply" ~>  "(" ~> repsep(factorVariable, ",") <~ ")") ^^ { 
    case headVariable ~ varList =>
      ImplyFactorFunction(headVariable, varList)
  }

}