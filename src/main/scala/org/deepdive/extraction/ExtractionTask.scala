package org.deepdive.extraction

import org.deepdive.settings.Extractor

case class ExtractionTask(extractor: Extractor)

case class ExtractionTaskResult(task: ExtractionTask, success: Boolean)