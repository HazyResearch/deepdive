package org.deepdive.extraction.datastore

import org.deepdive.Logging
import au.com.bytecode.opencsv.CSVReader
import scala.io.Source
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConversions._

object FileDataUtils extends Logging {

  def queryAsJson[A](filename: String, sep: Char)(block: Iterator[JsValue] => A) : A = {
    val reader = new CSVReader(Source.fromFile(filename).reader, sep)
    try {
      // TODO: Don't read this into memory.
      block(reader.readAll.map(_.toJson).iterator)
    } finally {
      reader.close()
    }
  }
    

}