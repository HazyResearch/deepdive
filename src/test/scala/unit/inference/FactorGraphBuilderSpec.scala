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

  describe("building variables for a tuple") {

    val schema = Map[String, String]("r1.c1" -> "Boolean", "r2.c1" -> "Boolean")    

    it("should work with query variables") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Nil), KnownFactorWeight(0), "weightPrefix")      
      val sampleRow = Map[String, Any]("r1.id" -> 0l)
      val variables = actor.buildVariablesForRow(sampleRow, factorDesc, 0)
      assert(variables.size == 1)
      // assert(variables.head == Variable(0, VariableDataType.Boolean, 0.0, false, true, "r1", "c1"))
    }

    it("should work with evidence variables") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Nil), KnownFactorWeight(0), "weightPrefix")      
      val sampleRow = Map[String, Any]("r1.id" -> 0l, "r1.c1" -> true)
      val variables = actor.buildVariablesForRow(sampleRow, factorDesc, 0)
      assert(variables.size == 1)
      // assert(variables.head == Variable(0, VariableDataType.Boolean, 1.0, true, false, "r1", "c1"))
    }

    it("should work with multiple variable columns") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Seq("r2.c1")), KnownFactorWeight(0), "weightPrefix")      
      val sampleRow = Map[String, Any]("r1.id" -> 0l, "r2.id" -> 1l, "r2.c1" -> true)
      val variables = actor.buildVariablesForRow(sampleRow, factorDesc, 0)
      assert(variables.size == 2)
      // assert(variables.toSet == Set(
      //   Variable(0, VariableDataType.Boolean, 0.0, false, true, "r1", "c1"),
      //   Variable(1, VariableDataType.Boolean, 1.0, true, false, "r2", "c1")))
    }

    it("should work with an array column") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1[]", Seq("r2.c1[]")), KnownFactorWeight(0), "weightPrefix")   
      val sampleRow = Map[String, Any]("r1.id" -> Array(0l,1l,2l), "r2.id" -> Array(3l,4l,5l), 
        "r2.c1" -> Array(true, false, true))
      val variables = actor.buildVariablesForRow(sampleRow, factorDesc, 0)
      assert(variables.size == 6)
    }

  }

  describe("building weights for a tuple") {
    
    val schema = Map[String, String]("r1.c1" -> "Boolean", "r2.c1" -> "Boolean") 

    it("should work for fixed weights") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Nil), KnownFactorWeight(100), "weightPrefix")
      val sampleRow = Map[String, Any]("r1.id" -> 0l)
      val weight = actor.buildWeightForRow(sampleRow, factorDesc)
      assert(weight.value == 100.0)
      assert(weight.isFixed)
    }

    it("should work for weights without variables") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Nil), UnknownFactorWeight(Nil), "weightPrefix")
      val sampleRow = Map[String, Any]("r1.id" -> 0l)
      val weight = actor.buildWeightForRow(sampleRow, factorDesc)
      assert(weight.value == 0.0)
      assert(!weight.isFixed)
    }

    it("should work for weights with variables") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Nil), UnknownFactorWeight(List("r1.value")), "weightPrefix")
      val sampleRow = Map[String, Any]("r1.id" -> 0l, "r1.value" -> "Hello")
      val weight = actor.buildWeightForRow(sampleRow, factorDesc)
      assert(weight.value == 0.0)
      assert(!weight.isFixed)
    }

  }

  describe("building factors for a tuple") {

    val schema = Map[String, String]("r1.c1" -> "Boolean", "r2.c1" -> "Boolean") 

    it("should work for simple factors") {
       val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
       val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Nil), UnknownFactorWeight(Nil), "weightPrefix")
       val weight = Weight(0l, 0.0, true, "someWeight")
       val sampleRow = Map[String, Any]("r1.id" -> 0l, "r1.value" -> "Hello")
       val factor = actor.buildFactorForRow(sampleRow, factorDesc, weight)
       assert(factor.weightId == weight.id)
       assert(factor.variables.size == 1)
    }

    it("should work for factors using array variables") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1[]", Seq("r2.c1[]")), KnownFactorWeight(0), "weightPrefix")   
      val sampleRow = Map[String, Any]("r1.id" -> Array(0l,1l,2l), "r2.id" -> Array(3l,4l,5l), 
        "r2.c1" -> Array(true, false, true))
      val weight = Weight(0l, 0.0, true, "someWeight")
      val factor = actor.buildFactorForRow(sampleRow, factorDesc, weight)
      assert(factor.weightId == weight.id)
      assert(factor.variables.size == 6)
    }
    
  }

  describe("generating a unqiue variable id") {

    val schema = Map[String, String]("r1.c1" -> "Boolean", "r2.c1" -> "Boolean", "r2.c2" -> "Boolean") 

    it("should work") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val variableIds = List(
        actor.generateVariableId(0,"r1.c1"), actor.generateVariableId(1,"r1.c1"), actor.generateVariableId(2,"r1.c1"), 
        actor.generateVariableId(0,"r2.c1"), actor.generateVariableId(1,"r2.c1"), actor.generateVariableId(2,"r2.c1"),
        actor.generateVariableId(0,"r2.c2"), actor.generateVariableId(1,"r2.c2"), actor.generateVariableId(2,"r2.c2"))
      assert(variableIds.toSet.size == 9)
    }

  }

  describe("processing a row") {

    val schema = Map[String, String]("r1.c1" -> "Boolean", "r2.c1" -> "Boolean", "r2.c2" -> "Boolean") 

    it("should work for simple factors") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1", Nil), UnknownFactorWeight(List("r1.value")), "weightPrefix")
      val sampleRow = Map[String, Any]("r1.id" -> 0l, "r1.value" -> "Hello")
      actor.processRow(sampleRow, factorDesc, 0)
      val dataStore = actor.inferenceDataStore
        .asInstanceOf[MemoryInferenceDataStoreComponent#MemoryInferenceDataStore]
      assert(dataStore.variables.size == 1)
      assert(dataStore.weights.size == 1)
      assert(dataStore.factors.size == 1)
    }

    it("should work for with array factor function variables") {
      val actor = TestActorRef[MemoryFactorGraphBuilder](actorProps(schema)).underlyingActor
      val factorDesc = FactorDesc("f1", "inputQuery", 
        ImplyFactorFunction("r1.c1[]", Seq("r2.c1[]")), KnownFactorWeight(0), "weightPrefix")  
      val sampleRow = Map[String, Any]("r1.id" -> Array(0l,1l,2l), "r2.id" -> Array(3l,4l,5l), 
        "r2.c1" -> Array(true, false, true))
      actor.processRow(sampleRow, factorDesc, 0)
      val dataStore = actor.inferenceDataStore
        .asInstanceOf[MemoryInferenceDataStoreComponent#MemoryInferenceDataStore]
      assert(dataStore.variables.size == 6)
      assert(dataStore.weights.size == 1)
      assert(dataStore.factors.size == 1)
    }

  }

  describe("adding factors and variables") {

    it("should work without batching")(pending)

    it("should work with batching")(pending)

  }

}