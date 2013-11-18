package org.deepdive.inference

import scala.collection.mutable.Map

class SerialMap {

  var currentOffset : Long = 0
  val parentOffsets = Map[String, Long]()

  def addParent(key: String, length: Long) {
    parentOffsets += Tuple2(key, currentOffset)
    currentOffset += length
  }

  def getGlobalId(key: String, localId: Long) = {
    for {
      offset <- parentOffsets.get(key)
      globalId <- Some(offset + localId) 
    } yield globalId
  }

}