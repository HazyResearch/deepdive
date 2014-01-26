package org.deepdive.settings

import org.deepdive.Logging
import scala.util.parsing.combinator.RegexParsers

object FactorFunctionParser extends RegexParsers with Logging {
  def relationOrField = """[\w]+""".r
  def arrayDefinition = """\[\]""".r
  // def factorFunctionName = "Imply" | "Or" | "And" | "Equal" | "IsTrue"

  def implyFactorFunction = "Imply" ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    ImplyFactorFunction(varList.last, varList.slice(0, varList.size-1))
  }

  def orFactorFunction = "Or" ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    OrFactorFunction(varList)
  }

  def andFactorFunction = "And" ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    AndFactorFunction(varList)
  }

  def equalFactorFunction = "Equal" ~> "(" ~> factorVariable ~ ("," ~> factorVariable) <~ ")" ^^ { 
    case v1 ~ v2 =>
    EqualFactorFunction(List(v1, v2))
  }

  def isTrueFactorFunction = "IsTrue" ~> "(" ~> factorVariable <~ ")" ^^ { variable =>
    IsTrueFactorFunction(List(variable))
  }

  
  def factorVariable = ("!"?) ~ rep1sep(relationOrField, ".") ~ (arrayDefinition?) ^^ { 
    case (isNegated ~ varList ~ isArray)  => 
      FactorFunctionVariable(varList.take(varList.size - 1).mkString("."), varList.last, 
        isArray.isDefined, isNegated.isDefined)
  }

  def factorFunc = implyFactorFunction | orFactorFunction | andFactorFunction | 
    equalFactorFunction | isTrueFactorFunction

}