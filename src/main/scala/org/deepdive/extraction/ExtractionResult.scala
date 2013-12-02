package org.deepdive.extraction

import spray.json._

case class ExtractionResult(rows: List[JsArray])