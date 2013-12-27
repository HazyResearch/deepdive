package org.deepdive.extraction

import spray.json._
import rx.lang.scala._

case class ExtractionResult(rows: Observable[JsObject])