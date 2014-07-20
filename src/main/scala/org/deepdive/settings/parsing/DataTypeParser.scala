package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object DataTypeParser extends RegexParsers {
  def CategoricalParser = "Categorical" ~> "(" ~> """\d+""".r <~ ")" ^^ { n => MultinomialType(n.toInt) }
  def BooleanParser = "Boolean" ^^ { s => BooleanType }
  def RealParser = "Real" ^^ { s => RealType }
  def ArrayRealParser = "ArrayReal" ^^ { s => ArrayRealType }
  def dataType = CategoricalParser | BooleanParser | RealParser | ArrayRealParser
}