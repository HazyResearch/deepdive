package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object DataTypeParser extends RegexParsers {
  def CategoricalParser = "Categorical" ~> "(" ~> """\d+""".r <~ ")" ^^ { n => MultinomialType(n.toInt) }
  def BooleanParser = "Boolean" ^^ { s => BooleanType }
  //def RealNumberParser = "RealNumber" ^^ {s=>RealNumberType }
  def RealArrayParser = "RealNumberArray" ~> "(" ~> """\d+""".r <~ ")" ^^ {n => RealArrayType(n.toInt)}
  def dataType = CategoricalParser | BooleanParser | RealArrayParser //|  RealNumberParser 
}