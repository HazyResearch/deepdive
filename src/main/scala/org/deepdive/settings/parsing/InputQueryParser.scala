package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers

object InputQueryParser extends RegexParsers {

  def filenameExpr =  "'" ~> """[^']+""".r <~ "'"
  def CSVInputQueryExpr = "CSV" ~> "(" ~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, ',') }
  def TSVInputQueryExpr = "TSV" ~> "("~> filenameExpr <~ ")" ^^ { str => CSVInputQuery(str, '\t') }
  def DatastoreInputQueryExpr = not("CSV") ~> not("TSV") ~> "[\\w\\W]+".r ^^ { str => 
    val withoutColon = """;\s+\n?$""".r.replaceAllIn(str, "")
    val result = """[\s\n]+""".r replaceAllIn(withoutColon, " ")
    DatastoreInputQuery(result) 
  }
  def inputQueryExpr = (CSVInputQueryExpr | TSVInputQueryExpr | DatastoreInputQueryExpr)

}

