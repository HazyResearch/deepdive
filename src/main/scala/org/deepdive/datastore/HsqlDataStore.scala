/*
package org.deepdive.datastore

import org.deepdive.Logging
import play.api.libs.json._
import scalikejdbc._

/* Helper object for working with HSQL */
object HSQLDataStore extends JdbcDataStore with Logging {

  def bulkInsertJSON(outputRelation: String, data: Iterator[JsObject])(implicit session: DBSession) = {
    bulkInsert(outputRelation, data.map(jsObjectToMap))
  }

  def jsObjectToMap(jsObj: JsObject) : Map[String, Any] = {
    jsObj.value.mapValues {
      case JsNull => null
      case JsString(x) => x
      case JsNumber(x) => x
      case JsBoolean(x) => x
      case JsArray(x) => 
        val elements = x.collect {
          case JsString(x) => x
          case JsNumber(x) => x.toString
          case JsBoolean(x) => x.toString
          case JsNull => "null"
        }
        new org.hsqldb.jdbc.JDBCArrayBasic(elements.toArray, org.hsqldb.types.Type.SQL_VARCHAR)
      case x : JsObject => jsObjectToMap(x)
      case JsUndefined() => null
    }.toMap
  }

}
*/