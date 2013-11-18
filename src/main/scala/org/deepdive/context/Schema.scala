// package org.deepdive.context

// object AttributeType extends Enumeration {
//   type AttributeType = Value
//   val `Integer`, `Long`, `String`, `Decimal`, `Float`, `Text`, `Timestamp`, `Boolean`, `Binary` = Value

//   def toScalaType(value: AttributeType) = {
//     value match {
//       case AttributeType.Integer => Int
//       case AttributeType.Long => Long
//       case AttributeType.String => String
//       case AttributeType.Decimal => Float
//       case AttributeType.Float => Float
//       case AttributeType.Text => String
//       case AttributeType.Timestamp => String
//       case AttributeType.Boolean => Boolean
//       case AttributeType.Binary => Array[Byte]_
//       case _ => String
//     }
//   }
// }
// import AttributeType._
