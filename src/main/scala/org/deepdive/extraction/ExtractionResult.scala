package org.deepdive.extraction

import play.api.libs.json._
import rx.lang.scala._

case class ExtractionResult(rows: Observable[Seq[JsObject]])