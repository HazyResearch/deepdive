package org.deepdive.settings

sealed trait InputQuery

case class CSVInputQuery(filename: String, seperator: Char) extends InputQuery
case class DatastoreInputQuery(query: String) extends InputQuery

object InputQuery {
  implicit def stringToInputquery(str: String) : InputQuery = {
    InputQueryParser.parse(InputQueryParser.inputQueryExpr, str).get
  }
}