package org.deepdive.context

/* Describes the context of the DeepDive application */
case class Context(
  sourceDatabaseUrl: String, 
  inferenceDatabaseUrl: String
)