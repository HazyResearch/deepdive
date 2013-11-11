package org.deepdive.extraction

import anorm._ 
import java.sql.Connection
import spray.json._
import org.deepdive.datastore.{PostgresDataStore => DB}
import scala.sys.process._
import scala.io.Source
import java.io.File

import DefaultJsonProtocol._

class ScriptTaskExecutor(task: ExtractionTask, databaseUrl: String) {

  def run() : List[JsValue] = {
    
    // Set the script to be executable
    val file = new File(task.udf)
    file.setExecutable(true)

    DB.withConnection { implicit conn =>

      // Query for the input data
      val inputData = SQL(task.inputQuery)().map { row =>
        row.data.map(_.toString).toList.toJson.compactPrint
      }

      // Result will be stored here
      var result : List[JsValue] = Nil;

      val io = new ProcessIO(
        in => { 
          inputData.foreach { x => in.write(x.getBytes) }
          in.close()
        },
        out => { 
          result = Source.fromInputStream(out).getLines().map(_.asJson).toList
          out.close()
        },
        err => { err.close() }
      )
      val process = task.udf run(io)
      process.exitValue()
      
      result
    }

  }

}