package org.deepdive.extraction

import anorm._ 
import java.io.{File, PrintWriter}
import java.sql.Connection
import org.deepdive.extraction.datastore._
import org.deepdive.Logging
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.sys.process._
import spray.json._

class ScriptTaskExecutor(task: ExtractionTask, 
  dataStoreComp: ExtractionDataStoreComponent) extends Logging { 

  def run() : ExtractionResult = {
    
    // Set the script to be executable
    val file = new File(task.extractor.udf)
    file.setExecutable(true)

    log.info(s"Running UDF: ${file.getAbsolutePath}")

    // Get the input data
    val inputData = dataStoreComp.dataStore.queryAsJson(task.extractor.inputQuery)

    // Result will be stored here
    val result : ArrayBuffer[JsValue] = ArrayBuffer[JsValue]();
      
    // Build the process
    val io = new ProcessIO(
      in => { 
        val writer = new PrintWriter(in, true)
        inputData.foreach { tuple => writer.println(tuple) }
        in.close()
      },
      out => {
        Source.fromInputStream(out).getLines.map(_.asJson).foreach { tuple => result += tuple }
        out.close()
      },
      err => {
        Source.fromInputStream(err).getLines.foreach(println)
      }
    ).daemonized()
    
    // Run the process
    val process = task.extractor.udf run(io)
    process.exitValue()

    log.info(s"UDF process has exited. Generated num=${result.size} records.")
    ExtractionResult(result.map(_.asInstanceOf[JsObject]).toList)
  }
}