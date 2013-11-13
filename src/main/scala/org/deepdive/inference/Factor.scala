package org.deepdive.inference

case class Factor(id: Integer, factorFunctionId: Integer, weight: Weight, variables: List[FactorVariable])
case class FactorVariable(position: Integer, positive: Boolean, value: Variable)