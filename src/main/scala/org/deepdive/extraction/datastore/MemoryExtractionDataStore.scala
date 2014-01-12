package org.deepdive.extraction.datastore

import org.deepdive.Logging
import scala.collection.mutable.{Map => MMap, ArrayBuffer}
import play.api.libs.json._

/* Stores Extraction Results */
trait MemoryExtractionDataStoreComponent extends ExtractionDataStoreComponent{

  val dataStore = new MemoryExtractionDataStore

  class MemoryExtractionDataStore extends ExtractionDataStore[JsObject] with Logging {
    
    def BatchSize = 100000
    
    val data = MMap[String, ArrayBuffer[JsObject]]()

    def init() = {
      data.clear()
    }

    def queryAsJson[A](query: String)(block: Iterator[JsObject] => A) : A = {
      block(data.get(query).map(_.toList).getOrElse(Nil).iterator)
    }
    
    def queryAsMap[A](query: String)(block: Iterator[Map[String, Any]] => A) : A = {
      queryAsJson(query) { iter => 
        block(iter.map(_.value.toMap.mapValues {
          case JsNull => null
          case JsString(x) => x
          case JsNumber(x) => x
          case JsBoolean(x) => x
          case _ => null
        }))
      }
    }
    
    def addBatch(result: Iterator[JsObject], outputRelation: String) : Unit = {
      //TODO: Use parallel collection
      data.synchronized {
        data.get(outputRelation) match {
          case Some(rows) => rows ++= result.toSeq
          case None => data += Tuple2(outputRelation, ArrayBuffer(result.toList: _*))
        }
      }
    }

  }
  
}