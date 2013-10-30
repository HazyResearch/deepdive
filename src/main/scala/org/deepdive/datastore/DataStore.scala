package org.deepdive.datastore

/** A generic data store, for example an RDBMS instance. 
  * 
  * DeepDive is architected in a way to support a variety of data stores.
  * This class defines the interface for a data store.
  */
abstract class DataStore {

  /* Returns true if the data store contains a relation with the given name */
  def hasRelationWithName(name: String) : Boolean

  /** Creates a new relation in the given DataStore. 
    * Does nothing if the relation already exists. 
    * Throws an exception in case of a schema mismatch. 
    */
  def createRelation(relation: Relation)

  /* Returns the schema of the data store */
  def getSchema(): Array[Relation]

}