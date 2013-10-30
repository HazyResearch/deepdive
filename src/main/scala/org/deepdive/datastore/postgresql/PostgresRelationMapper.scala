package org.deepdive.datastore.postgresql

import org.deepdive.datastore._
import scala.slick.jdbc.meta.MTable
import Domain._

object PostgresRelationMapper extends RelationMapper {

  def convert(table: MTable)(implicit session: scala.slick.session.Session) : Relation = {
    val tableName = table.name.name
    val columns = table.getColumns().list().map { column => 
      // TODO: Figure out types based on postgres types
      Attribute(column.column, Domain.String)
    }.toArray
    new Relation(tableName, columns)
  }

}