package org.deepdive.extraction.datastore

import org.deepdive.Logging
import scala.collection.mutable.{Map => MMap, ArrayBuffer}
import spray.json._

/* Stores Extraction Results */
trait MemoryExtractionDataStoreComponent extends ExtractionDataStoreComponent{

  val dataStore = new MemoryExtractionDataStore

  class MemoryExtractionDataStore extends ExtractionDataStore with Logging {
    
    def BatchSize = 100000
    
    val data = MMap[String, ArrayBuffer[JsObject]]()

    def init() = {}

    override def queryAsJson(relation: String) : Iterator[JsObject] = {
      data.get(relation).map(_.toList).getOrElse(Nil).iterator
    }
    
    def queryAsMap(relation: String) : Iterator[Map[String, Any]] = {
      queryAsJson(relation).map(_.fields.mapValues {
        case JsNull => null
        case JsString(x) => x
        case JsNumber(x) => x
        case JsBoolean(x) => x
        case _ => null
      })
    }
    
    def write(result: Seq[JsObject], outputRelation: String) : Unit = {
      data.get(outputRelation) match {
        case Some(rows) => rows ++= result
        case None => data += Tuple2(outputRelation, ArrayBuffer(result: _*))
      }
    }

  }
  
}