package org.deepdive.settings

/* Extractor specified in the settings */
case class LoaderConfig (
    connection: String,
    schemaFile: String,
    threads: Int,
    parallelTransactions: Int
)
