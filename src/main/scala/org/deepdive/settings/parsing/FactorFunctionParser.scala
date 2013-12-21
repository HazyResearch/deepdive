package org.deepdive.settings

import org.deepdive.Logging
import scala.util.parsing.combinator.RegexParsers

object FactorFunctionParser extends RegexParsers with Logging {
  def relationOrField = """[\w]+""".r
  def arrayDefinition = """\[\]""".r
  def factorFunctionName = "Imply" | "Dummy"

  
  def factorVariable = rep1sep(relationOrField, ".") ~ (arrayDefinition?) ^^ { 
    case (varList ~ isArray)  => 
      FactorFunctionVariable(varList.take(varList.size - 1).mkString("."), varList.last, isArray.isDefined)
  }

  def factorFunc = factorVariable ~ ("=" ~> factorFunctionName) ~ ("(" ~> repsep(factorVariable, ",")) <~ ")" ^^ { 
    case headVariable ~ functionName ~ varList =>
      functionName match {
        case "Imply" => ImplyFactorFunction(headVariable, varList)
        case _ => 
          log.error(s"Factor function not supported: ${functionName}")
          null
      }
      
  }

}