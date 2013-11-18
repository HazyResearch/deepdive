package org.deepdive.inference

case class Factor(id: Integer, factorFunction: FactorFunction, weight: Weight, variables: List[FactorVariable])
case class FactorVariable(position: Integer, positive: Boolean, value: Variable)
case class FactorFunction(id: Integer, desc: String)