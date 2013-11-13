package org.deepdive.datastore

import akka.actor.Actor
import java.sql.Connection

trait Connected { self: Actor =>

  implicit lazy val connection : Connection = PostgresDataStore.borrowConnection()

  override def postStop() {
    connection.close()
  }

}