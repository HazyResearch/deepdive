package org.deepdive.datastore

import java.sql.Connection
import scalikejdbc.ConnectionPool

abstract class DataStore {

  def withConnection[A](block: Connection => A): A

}