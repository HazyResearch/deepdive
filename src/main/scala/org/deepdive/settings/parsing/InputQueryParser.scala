package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers

object InputQueryParser extends RegexParsers {

  def filenameExpr =  "'" ~> """[^']+""".r <~ "'"
  def CSVInputQueryExpr = "CSV" ~> "(" ~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, ',') }
  def TSVInputQueryExpr = "TSV" ~> "("~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, '\t') }
  def DatastoreInputQueryExpr = not("CSV") ~> not("TSV") ~> "[\\w\\W]+".r ^^ { str => DatastoreInputQuery(str.replaceAll("\n", " ")) }
  def inputQueryExpr = (CSVInputQueryExpr | TSVInputQueryExpr | DatastoreInputQueryExpr)

}

