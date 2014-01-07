package org.deepdive.settings

/* Extraction Settings */
case class ExtractionSettings(extractors: List[Extractor], parallelism: Int)