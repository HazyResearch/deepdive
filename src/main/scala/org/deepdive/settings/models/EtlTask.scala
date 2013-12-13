package org.deepdive.settings

/* An ETL Task specified in the settings */
case class EtlTask(relation: String, source: String)