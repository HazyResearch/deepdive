package org.deepdive.datastore

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta._
import Database.threadLocalSession

/* A postgresql database */
class PostgresDataStore(val url: String) extends DataStore {

  object ColumnInfos extends Table[(String, String, String)]("information_schema.columns") {
    def table_name = column[String]("table_name")
    def column_name = column[String]("column_name")
    def data_type = column[String]("data_type")
    def * = table_name ~ column_name ~ data_type
  }

  def hasRelationWithName(name: String) : Boolean = {
    throw new java.lang.UnsupportedOperationException("Not yet implemented.")
  }
  
  def createRelation(relation: Relation) {
    throw new java.lang.UnsupportedOperationException("Not yet implemented.")
  }

  def getSchema() : Array[Relation] = {
    throw new java.lang.UnsupportedOperationException("Not yet implemented.")
    Database.forURL(url) withSession {
      MTable.getTables().list().foreach { table => 
        println(s"${table.name.name}\n")
        table.getColumns.list().foreach { column => 
          println(s"${column.column}:${column.typeName}")
        }
      }
    }
    return null;
  }

}