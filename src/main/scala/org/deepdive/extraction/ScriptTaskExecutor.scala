package org.deepdive.extraction

import java.io.{File, PrintWriter, OutputStream, InputStream}
import org.deepdive.Logging
import rx.lang.scala._
import rx.lang.scala.ImplicitFunctionConversions._
import rx.lang.scala.subjects._
import rx.{Observable => JObservable}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._
import scala.io.Source
import scala.sys.process._
import scala.util.Try
import spray.json._


class ScriptTaskExecutor(task: ExtractionTask, inputData: Iterator[JsObject]) extends Logging { 

  val POLL_TIMEOUT = 1.seconds

  def run() : ExtractionResult = {
    
    // Set the script to be executable
    val file = new File(task.extractor.udf)
    file.setExecutable(true)

    log.info(s"Running UDF: ${file.getAbsolutePath} with parallelism=${task.extractor.parallelism} " + 
      s"batch_size=${task.extractor.inputBatchSize}")

    // An input stream for each process
    val inputSubjects = (1 to task.extractor.parallelism).map ( i => ReplaySubject[JsObject]() )

    // Build process descriptions based on different data inputs
    val (processes, outputSubjects) = inputSubjects.zipWithIndex.map { case(inputSubject, i) =>
      val subjectOut = ReplaySubject[JsObject]()
      // Build and run the process
      val processBuilder = buildProcessIO(s"${task.extractor.name}[$i]", subjectOut, inputSubject)
      val process = task.extractor.udf run(processBuilder)
      (process, subjectOut)
    }.unzip


    // We merge all output streams into one stream
    val tmpObs = JObservable.from(outputSubjects.map(_.asJavaObservable).toIterable.asJava)
    val mergedObservables : Observable[JsObject] = JObservable.merge[JsObject](tmpObs)    

    // Send the input data in a batch-wise round-robin fashion.
    // We execute this on another thread, so that we don't block.
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val cyclingInput = Stream.continually(inputSubjects.toStream).flatten
      inputData.grouped(task.extractor.inputBatchSize).toStream.zip(cyclingInput).foreach { case(batch, obs) =>
        batch.foreach ( tuple => obs.onNext(tuple) )
      }
      inputSubjects.foreach { x => x.onCompleted() } 
    }  

    // We wait for the process on a separate thread
    processes.zip(outputSubjects).foreach { case(process, subj) =>
      Future {
        process.exitValue() match {
          case 0 =>  subj.onCompleted()
          case x => subj.onError(new RuntimeException(s"process had exit value $x"))
        }
      }
    }


    ExtractionResult(mergedObservables.buffer(task.extractor.outputBatchSize))
  }

  private def buildProcessIO(name: String, subject: ReplaySubject[JsObject], 
    input: ReplaySubject[JsObject]) = {
    new ProcessIO(
      in => handleProcessIOInput(in, name, input),
      out => handleProcessIOOutput(out, name, subject),
      err => {
        Source.fromInputStream(err).getLines.foreach(l => log.error(l))
      }
    )
  }

  private def handleProcessIOInput(in: OutputStream, name: String, 
     input: ReplaySubject[JsObject]) : Unit = {
    log.debug(s"${name} running")
    val writer = new PrintWriter(in, true)
    input.subscribe(
      tuple => writer.println(tuple.compactPrint),
      ex => {},
      () => in.close()
    )
    
  } 

  private def handleProcessIOOutput(out: InputStream, name: String, 
    subject: ReplaySubject[JsObject]) : Unit = {

    val jsonIterable = Source.fromInputStream(out).getLines.map { line =>
      Try(line.asJson.asJsObject).getOrElse {
        log.warning(s"Could not parse JSON: ${line}")
        JsObject()
      }
    }
    // We create an observable from the stream and subscribe the output to it
    // We only handle the onNext event. The onComplete is handled when the process exists.
    Observable(JObservable.from(jsonIterable.toIterable.asJava)).subscribe(
      input => subject.onNext(input)
    )
    out.close()
    log.debug(s"${name} done")   
  }

}