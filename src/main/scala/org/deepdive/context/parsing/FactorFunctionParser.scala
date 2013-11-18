package org.deepdive.context.parsing

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.context.ImplyFactorFunction

object FactorFunctionParser extends RegexParsers {
  def factorVariable = """\w+""".r
  def factorFunc = "Imply" ~>  "(" ~> repsep(factorVariable, ",") <~ ")" ^^ { case varList =>
    ImplyFactorFunction(varList)
  }

}