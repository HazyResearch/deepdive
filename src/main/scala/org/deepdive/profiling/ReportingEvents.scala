package org.deepdive.profiling

sealed trait ReportingEvent
case class StartReport(id: String, description: String) extends ReportingEvent
case class EndReport(id: String, description: Option[String]) extends ReportingEvent
case class QuickReport(id: String, description: String) extends ReportingEvent