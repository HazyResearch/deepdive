package org.deepdive.profiling

import akka.actor.{Actor, ActorLogging, Props}
import scala.collection.mutable.{ArrayBuffer, Map}
import org.deepdive.settings._
import java.util.Date
import scala.util.{Try, Success}

object Profiler {
  def props = Props(classOf[Profiler])

  sealed trait Message 
  case object PrintReports extends Message

  case class Report(id: String, startDescription: String, endDescription: String, 
    startTime: Long, endTime: Long)
}

class Profiler extends Actor with ActorLogging {

  import Profiler._

  val reports = ArrayBuffer[Report]()
  val startedReports = Map[String, (StartReport, Long)]()

  override def preStart() {
    log.info(s"starting at ${self.path}")
    context.system.eventStream.subscribe(self, classOf[ReportingEvent])
  }

  def receive = {
    case QuickReport(id, description) =>
      reports += Report(id, description, "", System.currentTimeMillis, System.currentTimeMillis)
    case msg @ StartReport(id, startDescription) =>
      log.debug(s"starting report_id=${id}")
      startedReports += Tuple2(id, Tuple2(msg, System.currentTimeMillis))
    case EndReport(id, endDescription) =>
      startedReports.get(id) match {
        case Some((startReport, startTime)) =>
          log.debug(s"ending report_id=${id}")
          val finalReport = Report(id, startReport.description, 
            endDescription.map(" " + _).getOrElse(""), 
            startTime, System.currentTimeMillis)
          reports += finalReport
        case None =>
          log.warning(s"report_id=${id} was not started")
      }
    case PrintReports =>
      printReports()
      sender ! Success()
  }

  private def printReports() = {
    log.info("--------------------------------------------------")
    log.info("Summary Report")
    log.info("--------------------------------------------------")
    reports.sortBy(_.endTime).foreach { report =>
      val ms = (report.endTime - report.startTime)
      log.info(s"${report.startDescription}${report.endDescription} [${ms} ms]")
    }
    log.info("--------------------------------------------------")
  }



}


