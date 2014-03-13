package org.deepdive.inference

import org.deepdive.Logging
import org.deepdive.serialization.InferenceResultProtos
import java.io.FileInputStream

class ProtobufInferenceResultDeserializier extends Logging {

  def getWeights(fileName: String) : Iterator[WeightInferenceResult] = {
    val stream = new FileInputStream(fileName)
    Iterator.continually {
      InferenceResultProtos.WeightInferenceResult.parseDelimitedFrom(stream)
    }.takeWhile(_ != null).map { w =>
      WeightInferenceResult(w.getId, w.getValue)
    }
  }

  def getVariables(fileName: String) : Iterator[VariableInferenceResult] = {
    val stream = new FileInputStream(fileName)
    Iterator.continually {
      InferenceResultProtos.VariableInferenceResult.parseDelimitedFrom(stream)
    }.takeWhile(_ != null).map { v =>
      VariableInferenceResult(v.getId, v.getCategory, v.getExpectation)
    }
  }

}