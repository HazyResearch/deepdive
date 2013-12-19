package org.deepdive.test.unit

import anorm._
import org.deepdive.datastore.PostgresDataStore
import org.scalatest._
import org.deepdive._
import org.deepdive.Context
import org.deepdive.settings._
import org.deepdive.inference.{FactorGraphBuilder}
import org.deepdive.test._
import akka.actor._
import akka.testkit.{TestActorRef, TestKit}

class PostgresFactorGraphBuilderSpec extends FunSpec {

  implicit val system = ActorSystem("Test")

  def prepareData() {
    PostgresTestDataStore.init()
    PostgresDataStore.withConnection { implicit conn =>
     SQL("drop schema if exists public cascade; create schema public;").execute()
     SQL("create table entities (id bigserial primary key, word_id integer, is_present boolean)").execute()
     SQL("""create table parents(id bigserial primary key, entity1_id integer, 
        entity2_id integer, meta text, is_true boolean)""").execute()
     SQL("""insert into entities(word_id, is_present) VALUES (1, true), (2, true), (3, true), 
        (4, true), (5, true), (6, true);""").execute()
     SQL("""insert into parents(entity1_id, entity2_id, meta, is_true) 
        VALUES (1, 2, 'A', NULL), (2, 3, 'B', NULL), (1, 5, 'C', NULL);""").execute()
   }
  }


  describe("addVariableAndFactorsForRelation") {
    
    it("should work") {
      prepareData()
      
      val actor = TestActorRef[FactorGraphBuilder.PostgresFactorGraphBuilder].underlyingActor

      // Add Factors and Variables for the entities relation
      val entityRelation = Relation("entities", Map("id" -> "Long", "word_id" -> "Integer", 
        "is_present" -> "Boolean"))
      val entityFactorDesc = FactorDesc("entititiesFactor", "SELECT * FROM entities", 
        ImplyFactorFunction("entities.is_present", Nil), KnownFactorWeight(1.0))
      actor.addFactorsAndVariables(entityFactorDesc, 0.0)
      // Should have one variable and fact,or for each tuple
      // assert(actor.inferenceDataStore.variables.size == 6)
      // assert(actor.inferenceDataStore.factors.size == 6)
      // actor.inferenceDataStore.flush()
      // assert(actor.inferenceDataStore.variables.size == 0)
      // assert(actor.inferenceDataStore.factors.size == 0)
      // assert(actor.inferenceDataStore.variableIdMap.size == 6)

      // Add Factors and Variables for the parents relations
      val parentsRelation = Relation("parents",
        Map("id" -> "Long", "entity1_id" -> "Long", "entity2_id" -> "Long", "is_true" -> "Boolean")
      )
      val parentsFactorDesc = FactorDesc(
        "parentsFactor", 
        """SELECT parents.*, e1.id AS "e1.id", e1.is_present AS "e1.is_present",
        e2.id as "e2.id", e2.is_present AS "e2.is_present"
        FROM parents
        INNER JOIN entities e1 ON parents.entity1_id = e1.id
        INNER JOIN entities e2 ON parents.entity2_id = e2.id
        """,
        ImplyFactorFunction("parents.is_true", List("entities.e1.is_present", "entities.e2.is_present")), 
        UnknownFactorWeight(List("parents.entity1_id"))
      )

      actor.addFactorsAndVariables(parentsFactorDesc, 0.0)
      // assert(actor.inferenceDataStore.variables.size == 3)
      // assert(actor.inferenceDataStore.factors.size == 3)
      // actor.inferenceDataStore.flush()
      // assert(actor.inferenceDataStore.variables.size == 0)
      // assert(actor.inferenceDataStore.factors.size == 0)
      // assert(actor.inferenceDataStore.variableIdMap.size == 9)

      // Make sure the data in the RDBMS is correct.
      PostgresDataStore.withConnection { implicit conn =>
        val factorCount = SQL("select count(*) as c from factors")().head[Long]("c")
        assert(factorCount == 9)
        val variableCount = SQL("select count(*) as c from variables")().head[Long]("c")
        assert(variableCount == 9)
        val weightCount = SQL("select count(*) as c from weights")().head[Long]("c")
        assert(weightCount == 3)
      }
    }
      
  }

}