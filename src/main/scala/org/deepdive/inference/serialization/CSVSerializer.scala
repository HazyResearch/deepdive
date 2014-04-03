// package org.deepdive.inference

// import org.deepdive.Logging
// import org.deepdive.settings._
// import java.io._

// class CSVSerializer(weightsOuput: OutputStream, variablesOutput: OutputStream, 
//   factorsOutput: OutputStream, edgesOutput: OutputStream, metaDataOutput: OutputStream) extends Serializer with Logging {

//   val printStream_weight = new PrintStream(weightsOuput)
//   val printStream_variable = new PrintStream(variablesOutput)
//   val printStream_factor = new PrintStream(factorsOutput)
//   val printStream_edge = new PrintStream(edgesOutput)
//   val printStream_meta = new PrintStream(metaDataOutput)



//   def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double, desc: String) : Unit = {
//     var out = weightId.toString + "," + isFixed.toString + ","
//     if (isFixed) out += initialValue.toString
//     else out += "0"
//     out += "," + desc + "\n"
//     printStream_weight.print(out)
//   }

//   def addVariable(variableId: Long, isEvidence: Boolean, initialValue: Double, 
//     dataType: String, edgeCount: Long, cardinality: Long) : Unit = {

//     val variableDataType = dataType match {
//       case "Boolean" => 'B'
//       case "Multinomial" => 'M'
//     } 
//     val out = variableId.toString + "," + isEvidence.toString + "," +
//       initialValue.toString + "," + variableDataType + "," + edgeCount.toString + 
//       "," + cardinality.toString + "\n"
//     printStream_variable.print(out)
//   }

//   def addFactor(factorId: Long, weightId: Long, factorFunction: String, edgeCount: Long) : Unit = {
//     val factorFunctionType = factorFunction match {
//       case "ImplyFactorFunction" => 'I'
//       case "OrFactorFunction" => 'O'
//       case "AndFactorFunction" => 'A'
//       case "EqualFactorFunction" => 'E'
//       case "IsTrueFactorFunction" =>  'I'
//     }
//     val out = factorId.toString + "," + weightId.toString + "," +
//       factorFunctionType + "," + edgeCount.toString + "\n"
//     printStream_factor.print(out)
//   }


//   def addEdge(variableId: Long, factorId: Long, position: Long, isPositive: Boolean, 
//     equalPredicate: Long) : Unit = {

//     val out = variableId.toString + "," + factorId.toString + "," + position.toString + 
//       "," + isPositive.toString + "," + equalPredicate.toString + "\n"
//     printStream_edge.print(out)
//   }

//   def writeMetadata(numWeights: Long, numVariables: Long, numFactors: Long, numEdges: Long,
//     weightsFile: String, variablesFile: String, factorsFile: String, edgesFile: String) : Unit = {
//     val out = numWeights.toString + "," + numVariables + "," + numFactors + 
//       "," + numEdges + "," + weightsFile + "," + variablesFile + "," + 
//       factorsFile + "," + edgesFile + "\n"
//     printStream_meta.print(out)
//   }

//   def close() : Unit = {
//     weightsOuput.flush()
//     variablesOutput.flush()
//     factorsOutput.flush()
//     edgesOutput.flush()
//     metaDataOutput.flush()

//   }

// }