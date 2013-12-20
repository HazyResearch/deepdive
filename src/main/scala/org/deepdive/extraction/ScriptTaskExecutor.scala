package org.deepdive.extraction

import java.io.{File, PrintWriter, OutputStream, InputStream}
import org.deepdive.Logging
import scala.collection.mutable.{ArrayBuffer, SynchronizedBuffer}
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration._
import scala.io.Source
import scala.sys.process._
import scala.util.Try
import spray.json._

class ScriptTaskExecutor(task: ExtractionTask, inputData: Stream[JsObject]) extends Logging { 

  val POLL_TIMEOUT = 1.seconds

  def run() : ExtractionResult = {
    
    // Set the script to be executable
    val file = new File(task.extractor.udf)
    file.setExecutable(true)

    log.info(s"Running UDF: ${file.getAbsolutePath} with parallelism=${task.extractor.parallelism} " + 
      s"batch_size=${task.extractor.batchSize}")

    // Create one input queue for each worker 
    val inputQueues = (1 to task.extractor.parallelism).map { x =>
      new SynchronousQueue[List[JsObject]]()
    }

    // Result will be stored here
    val result : ArrayBuffer[JsValue] = new ArrayBuffer[JsValue] with SynchronizedBuffer[JsValue]
    val isDone = new AtomicBoolean(false)
    
    // Build process descriptions based on different queues
    val processDescs = inputQueues.zipWithIndex.map { case(inputQueue, i) =>
      buildProcessIO(s"${task.extractor.name}[$i]", inputQueue, isDone, result)
    }
    
    // Run the processes
    val processes = processDescs.map { io =>
      task.extractor.udf run(io)
    }

    // Put data into the queue
    val queueStream = Stream.continually(inputQueues.toStream).flatten
    inputData.iterator.grouped(task.extractor.batchSize).toStream.zip(queueStream).foreach { case(batch, q) =>
      q.put(batch.toList)
    }
    isDone.set(true)

    val exitValues = processes.map(_.exitValue()).toSet
    if (exitValues.contains(1)) {
      log.error(s"${task.extractor.name} FAILED.")
      throw new RuntimeException(s"${task.extractor.name} FAILED.")
    } else {
      log.info(s"UDF process has exited. Generated num=${result.size} records.")
      ExtractionResult(result.map(_.asInstanceOf[JsObject]).toList)
    }
  }

  private def buildProcessIO(name: String, inputQueue: SynchronousQueue[List[JsObject]], 
    isDone: AtomicBoolean, result: ArrayBuffer[JsValue]) = {
    new ProcessIO(
      in => handleProcessIOInput(in, name, inputQueue, isDone),
      out => handleProcessIOOutput(out, name, result),
      err => {
        Source.fromInputStream(err).getLines.foreach(l => log.error(l))
      }
    ).daemonized()
  }

  private def handleProcessIOInput(in: OutputStream, name: String, 
    inputQueue: SynchronousQueue[List[JsObject]], isDone: AtomicBoolean) : Unit = {
    log.debug(s"${name} running")
    val writer = new PrintWriter(in, true)
    while(!isDone.get()) {
      Option(inputQueue.poll(POLL_TIMEOUT.length, POLL_TIMEOUT.unit)).getOrElse(Nil).foreach { tuple =>
        writer.println(tuple.compactPrint)
        // log.debug(s"${name}: ${tuple.compactPrint}")
      }
    }
    in.close()
  } 

  private def handleProcessIOOutput(out: InputStream, name: String, 
    result: ArrayBuffer[JsValue]) : Unit = {
    Source.fromInputStream(out).getLines.map { line =>
      Try(line.asJson.asJsObject).getOrElse {
        log.warning(s"Could not parse JSON: ${line}")
        JsObject()
      }
    }.foreach { tuple => result += tuple }
    out.close()
    log.debug(s"${name} done")
  }

}