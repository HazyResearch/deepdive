package org.deepdive.test

import anorm._
import org.deepdive.datastore.PostgresDataStore
import org.scalatest._
import org.deepdive._
import org.deepdive.context.{Relation, Factor => FactorDesc, ForeignKey}
import org.deepdive.inference.{FactorGraphBuilder}
import akka.actor._
import akka.testkit.{TestActorRef, TestKit}

class FactorGraphBuilderSpec extends FunSpec {

  implicit val system = ActorSystem("Test")

  def prepareData() {
    PostgresDataStore.init("jdbc:postgresql://localhost/deepdive_test", "dennybritz", "")
    PostgresDataStore.withConnection { implicit conn =>
     SQL("drop schema if exists public cascade; create schema public;").execute()
     SQL("create table entities (id bigserial primary key, word_id integer)").execute()
     SQL("""create table parents(id bigserial primary key, entity1_id integer, 
        entity2_id integer, meta text)""").execute()
     SQL("insert into entities(word_id) VALUES (1), (2), (3), (4), (5), (6);").execute()
     SQL("""insert into parents(entity1_id, entity2_id, meta) 
        VALUES (1, 2, 'A'), (2, 3, 'B'), (6, 5, 'C');""").execute()
   }
  }


  describe("addVariableAndFactorsForRelation") {
    
    it("should work") {
      prepareData()
      
      val actor = TestActorRef[FactorGraphBuilder].underlyingActor

      // Add Factors and Variables for the entities relation
      val entityRelation = Relation("entities", Map("id" -> "Long", "word_id" -> "Integer"), Nil)
      val entityFactorDesc = FactorDesc("entitity", ImplyFactorFunction(Nil), "?")
      actor.addVariableAndFactorsForRelation(entityRelation, entityFactorDesc)
      // Should have one variable and factor for each tuple
      assert(actor.variables.size == 6)
      assert(actor.factors.size == 6)
      // We have seen one factor function
      assert(actor.factorFunctions.size == 1)

      // Add Factors and Variables for the parents relations
      val parentsRelation = Relation("parents",
        Map("id" -> "Long", "entity1_id" -> "Long", "entity2_id" -> "Long"),
        List(ForeignKey("parents", "entity1_id", "entities", "id"), 
          ForeignKey("parents", "entity2_id", "entities", "id"))
      )
      val parentsFactorDesc = FactorDesc("parent", 
        ImplyFactorFunction(List("entity1_id", "entity2_id")), "?")
      actor.addVariableAndFactorsForRelation(parentsRelation, parentsFactorDesc)
      assert(actor.variables.size == 9)
      assert(actor.factors.size == 9)
      assert(actor.factorFunctions.size == 2)

      // TODO: Make sure the factors are correct

      // Insert into database
      actor.writeToDatabase()
    }
      
  }

}