package org.deepdive.profiling

import akka.actor.{Actor, ActorLogging, Props}
import scala.collection.mutable.ArrayBuffer
import org.deepdive.settings._
import java.util.Date

object Profiler {
  def props = Props(classOf[Profiler])

  case class ExtractorFinished(extractor: Extractor, startTime: Long, endTime: Long)
  case class ExtractorFailed(extractor: Extractor, startTime: Long, endTime: Long, exception: Throwable)
  case class FactorAdded(factorDesc: FactorDesc, startTime: Long, endTime: Long)
  case class SamplerFinished(startTime: Long, endTime: Long)
  case object Report
  case class TaskReport(startTime: Long, endTime: Long, desc: String)
}

class Profiler extends Actor with ActorLogging {

  import Profiler._

  val reports = ArrayBuffer[TaskReport]()

  // val finishedExtractors = ArrayBuffer[ExtractorFinished]()
  // val failedExtractors = ArrayBuffer[ExtractorFailed]()
  // val finishedFactors = ArrayBuffer[FactorAdded]()
  // val finishedSampler = ArrayBuffer[SamplerFinished]()

  override def preStart() {
    log.info(s"starting at ${self.path}")
  }

  def receive = {
    case ExtractorFinished(extractor, startTime, endTime) =>
      reports += TaskReport(startTime, endTime, s"extractor ${extractor.name}")
    case ExtractorFailed(extractor, startTime, endTime, exception) =>
      reports += TaskReport(startTime, endTime, s"extractor ${extractor.name} FAILED")
    case FactorAdded(factorDesc, startTime, endTime) =>
      reports += TaskReport(startTime, endTime, s"adding ${factorDesc.name} to factor graph")
    case SamplerFinished(startTime, endTime) =>
      reports += TaskReport(startTime, endTime, "sampling")
    case Report =>
      doReport()
      sender ! "Done"
  }

  private def doReport() : Unit = {
    log.info("Pipeline Summary:")
    reports.sortBy(_.endTime).foreach { report =>
      val ms = (report.endTime - report.startTime)
      log.info(s"${report.desc} (${ms} ms)")
    }
  }



}


