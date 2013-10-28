package org.deepdive.datastore

/* The domain an attribute can take on */
object Domain extends Enumeration {
  type Domain = Value
  val Integer, `String`, `Decimal`, `Float`, Text, Timestamp, `Boolean`, Binary = Value
}