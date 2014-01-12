package org.deepdive.extraction

import org.deepdive.Task
import org.deepdive.settings.Extractor
import scala.util.Try

case class ExtractionTask(extractor: Extractor)
case class ExtractionTaskResult(extractorName: String)