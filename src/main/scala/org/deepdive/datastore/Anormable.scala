package org.deepdive.datastore

import anorm._

trait Anormable[T] {
  implicit def toAnormSeq() : Seq[(String, ParameterValue[_])]
}

