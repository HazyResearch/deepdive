package org.deepdive.datastore.postgresql

import org.deepdive.datastore._
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
    Database.forURL(url) withSession {
      return MTable.getTables().list().map(PostgresRelationMapper.convert(_)).toArray
    }
  }

}