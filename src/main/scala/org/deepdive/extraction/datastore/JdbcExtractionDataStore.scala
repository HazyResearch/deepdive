package org.deepdive.extraction.datastore

import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import play.api.libs.json._
import scalikejdbc._
import java.io.BufferedReader
import scala.io.Source

trait JdbcExtractionDataStore extends ExtractionDataStore[JsObject] with Logging {

  def ds : JdbcDataStore

  val variableIdCounter = new java.util.concurrent.atomic.AtomicLong(0) 

  def queryAsMap[A](query: String, batchSize: Option[Int] = None)
      (block: Iterator[Map[String, Any]] => A) : A = {
      ds.DB.readOnly { implicit session =>
        session.connection.setAutoCommit(false)
        val stmt = session.connection.createStatement(
          java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY)
        stmt.setFetchSize(10000)
        try {
          // stmt.executeUpdate("ANALYZE");
          log.debug(query)
          val expQuery = "EXPLAIN " + query
          val ex = stmt.executeQuery(expQuery)
          log.debug(ex.getMetaData().getColumnLabel(1))
          while (ex.next()) {
            log.debug(ex getString 1)
          }

          val rs = stmt.executeQuery(query)
          // No result return
          if (!rs.isBeforeFirst) {
            log.warning(s"query returned no results: ${query}")
            block(Iterator.empty)
          } else {
            val resultIter = new Iterator[Map[String, Any]] {
              def hasNext = {
                // TODO: This is expensive
                !(rs.isLast)
              }              
              def next() = {
                rs.next()
                val metadata = rs.getMetaData()
                (1 to metadata.getColumnCount()).map { i => 
                  val label = metadata.getColumnLabel(i)
                  val data = unwrapSQLType(rs.getObject(i))
                  (label, data)
                }.filter(_._2 != null).toMap
              }
            }
            block(resultIter)
          }
        } catch {
          // SQL cmd exception
          case exception : Throwable =>
            log.error(exception.toString)
            throw exception
        }
      }
    }

    def queryAsJson[A](query: String, batchSize: Option[Int] = None)
      (block: Iterator[JsObject] => A) : A = {
      queryAsMap(query, batchSize) { iter =>
        val jsonIter = iter.map { row =>
          JsObject(row.mapValues(anyValToJson).toSeq)
        }
        block(jsonIter)
      }
    }

    def queryUpdate(query: String) {
      val conn = ds.borrowConnection()
      //conn.setAutoCommit(false);
      val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)
      try {
        val prep = conn.prepareStatement(query)
        prep.executeUpdate
      } catch {
        // SQL cmd exception
        case exception : Throwable =>
          log.error(exception.toString)
          throw exception
      } finally {
        conn.close()
      }
    }

    def unwrapSQLType(x: Any) : Any = {
      x match {
        case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray().asInstanceOf[Array[_]].toList
        case x : org.postgresql.util.PGobject =>
          x.getType match {
            case "json" => Json.parse(x.getValue)
            case _ => JsNull
          }
        case x => x
      }
    }

    /* Translates an arbitary values that comes back from the database to a JSON value */
    def anyValToJson(x: Any) : JsValue = x match {
      case Some(x) => anyValToJson(x)
      case None | null => JsNull
      case x : String => JsString(x)
      case x : Boolean => JsBoolean(x)
      case x : Int => JsNumber(x)
      case x : Long => JsNumber(x)
      case x : Double => JsNumber(x)
      case x : java.sql.Date => JsString(x.toString)
      case x : Array[_] => JsArray(x.toList.map(x => anyValToJson(x)))
      case x : List[_] => JsArray(x.toList.map(x => anyValToJson(x)))
      case x : JsObject => x      case x =>
        log.error(s"Could not convert ${x.toString} of type=${x.getClass.getName} to JSON")
        JsNull
    }

}