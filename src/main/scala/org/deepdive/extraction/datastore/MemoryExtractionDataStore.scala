package org.deepdive.extraction.datastore

import org.deepdive.Logging
import scala.collection.mutable.{Map => MMap, ArrayBuffer}
import spray.json._

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
        block(iter.map(_.fields.mapValues {
          case JsNull => null
          case JsString(x) => x
          case JsNumber(x) => x
          case JsBoolean(x) => x
          case _ => null
        }))
      }
    }
    
    def addBatch(result: Seq[JsObject], outputRelation: String) : Unit = {
      data.get(outputRelation) match {
        case Some(rows) => rows ++= result
        case None => data += Tuple2(outputRelation, ArrayBuffer(result: _*))
      }
    }

    def flushBatches(outputRelation: String) : Unit = {
      // Do nothing, already flushed the batch
    }

  }
  
}