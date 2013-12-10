package org.deepdive.test.unit

import com.typesafe.config._
import org.deepdive.settings._
import org.deepdive.context._
import org.scalatest._

class SettingsSpec extends FunSpec {

  def settingsFile = """
  deepdive {
    
    global.connection: {
      host: "localhost"
      port: 5432
      db: "deepdive_test"
      user: "root"
      password: "password"
    }

    relations.documents.schema: { id: Integer, text: Text, meta: Text }
    relations.entities.schema: { id: Integer, document_id: Integer, name: String, meta: Text }
    relations.entities.fkeys: { document_id: documents.id }

    extractions: {
      extractor1.output_relation: "entities"
      extractor1.input: "SELECT * FROM documents"
      extractor1.udf: "udf/entities.py"
    }

    factors: {
      entities.relation = "entities"
      entities.function: "id = Imply()"
      entities.weight: "?"
    }
  }
"""

  describe("Settings") {

    it("should parse a simple configuration file") {
      val settings = Settings.loadFromConfig(ConfigFactory.parseString(settingsFile))

      assert(settings.connection == Connection("localhost", 5432, "deepdive_test", 
        "root", "password"))
      
      assert(settings.relations.toSet == Set(
        Relation(
          "documents", 
          Map[String,String]("id" -> "Integer", "text" -> "Text", "meta" -> "Text"), 
          List[ForeignKey](ForeignKey("documents","id","documents","id")),
          None),
        Relation("entities", 
          Map[String,String]("id" -> "Integer", "document_id" -> "Integer", 
            "name" -> "String", "meta" -> "Text"), 
          List[ForeignKey](
            ForeignKey("entities","document_id","documents","id"),
            ForeignKey("entities","id","entities","id")),
          None)
      ))

      assert(settings.extractors == List(
        Extractor("extractor1", "entities", "SELECT * FROM documents", "udf/entities.py")
      ))

      assert(settings.factors == List(
        FactorDesc("entities", "entities",
          ImplyFactorFunction(FactorFunctionVariable(None, "id"), Nil), 
          UnknownFactorWeight(Nil))
      ))

    }

  }

}