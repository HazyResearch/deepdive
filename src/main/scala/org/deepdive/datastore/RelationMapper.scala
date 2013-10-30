package org.deepdive.datastore

import scala.slick.jdbc.meta.MTable
import Domain._

/* Maps data store relations types to DeepDive relations */
trait RelationMapper {

  def convert(table: MTable)(implicit session: scala.slick.session.Session) : Relation

}