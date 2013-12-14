package org.deepdive.settings

/* A relation specified in the settings */
case class Relation(name: String, schema: Map[String, String])

/* Foreign key of a relation in the settings */
case class ForeignKey(childRelation: String, childAttribute: String, parentRelation: String, 
  parentAttribute: String)