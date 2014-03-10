package org.deepdive.inference

import org.deepdive.calibration._
import org.deepdive.datastore.HSQLDataStore
import org.deepdive.Logging

/* Stores the factor graph and inference results in a postges database. */
trait HSQLInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  lazy val inferenceDataStore = new HSQLDataStoreInferenceDataStore

  class HSQLDataStoreInferenceDataStore extends SQLInferenceDataStore with Logging {

    def ds = HSQLDataStore
    override def keyType = "identity"
    override def stringType = "longvarchar"
    override def randomFunc = "RAND()"
    override def alterSequencesSQL = ""

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
    
    def bulkCopyWeights(weightsFile: String) : Unit = {
      
    }

    def bulkCopyVariables(variablesFile: String) : Unit = {
    
    }
  
  }
}
