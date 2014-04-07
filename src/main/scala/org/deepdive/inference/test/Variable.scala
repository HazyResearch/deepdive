package org.deepdive.inference.test

case class Variable(id: Long, isEvidence: Boolean, initialValue: Double, 
  dataType: Short, edgeCount: Long, cardinality: Long) {}