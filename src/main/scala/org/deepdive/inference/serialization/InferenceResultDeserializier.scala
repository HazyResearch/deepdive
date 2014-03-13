package org.deepdive.inference

trait InferenceResultDeserializier {

  def getWeights(fileName: String) : Iterator[WeightInferenceResult]
  def getVariables(filename: String) : Iterator[VariableInferenceResult]

}