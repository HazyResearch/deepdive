package org.deepdive.test.unit

import akka.actor._
import org.scalatest._
import org.deepdive.settings._
import org.deepdive.inference._
import org.deepdive.extraction.datastore._
import akka.testkit._

class MemoryFactorGraphBuilder(val variableSchema: Map[String, String]) 
  extends FactorGraphBuilder with MemoryExtractionDataStoreComponent
  with MemoryInferenceDataStoreComponent

class FactorGraphBuilderSpec(_system: ActorSystem) extends TestKit(_system) with FunSpecLike 
  with BeforeAndAfter {

  def this() = this(ActorSystem("FactorGraphBuilderSpec"))
  def actorProps(schema: Map[String, String]) = Props(classOf[MemoryFactorGraphBuilder], schema)

}