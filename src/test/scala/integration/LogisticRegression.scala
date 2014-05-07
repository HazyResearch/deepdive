// package org.deepdive.test.integration

// import anorm._ 
// import com.typesafe.config._
// import org.deepdive.test._
// import org.deepdive.Context
// import org.deepdive._
// import org.deepdive.datastore.{PostgresDataStore, JdbcDataStore}
// import org.scalatest._
// import scalikejdbc.ConnectionPool

// class LogisticRegressionApp extends FunSpec {

//   def prepareData() {
//     JdbcDataStore.init(ConfigFactory.load)
//     PostgresDataStore.withConnection { implicit conn =>
//        SQL("drop schema if exists public cascade; create schema public;").execute()
//        SQL("create table titles(id bigint, title text, has_extractions boolean);").execute()
//        SQL("""create table word_presences(id bigint, 
//         title_id bigint, word text, is_present boolean);""").execute()
//     }
//     JdbcDataStore.close()
//   }

//   def getConfig = {
//     val sqlQuery = s"""${"\"\"\""}
//       SELECT word_presences.id AS "word_presences.id", word_presences.is_present AS "word_presences.is_present", 
//       titles.id as "titles.id", titles.has_extractions AS "titles.has_extractions" ,
//       word_presences.word AS "word_presences.word"
//       FROM word_presences INNER JOIN titles ON word_presences.title_id = titles.id${"\"\"\""}"""

//     s"""
//       deepdive.schema.variables {
//         word_presences.is_present: Boolean
//         titles.has_extractions: Boolean
//       }

//       deepdive.extraction.extractors: {
//         titlesLoader.output_relation: "titles"
//         titlesLoader.input: "CSV('${getClass.getResource("/logistic_regression/titles.csv").getFile}')"
//         titlesLoader.udf: "${getClass.getResource("/logistic_regression/title_loader.py").getFile}"
//         titlesLoader.after: "util/fill_sequence.sh titles id"
        
//         wordsExtractor.output_relation: "word_presences"
//         wordsExtractor.input: "SELECT * FROM titles"
//         wordsExtractor.udf: "${getClass.getResource("/logistic_regression/word_extractor.py").getFile}"
//         wordsExtractor.dependencies = ["titlesLoader"]
//       }

//       deepdive.inference.factors {
//         wordFactor.input_query = ${sqlQuery}
//         wordFactor.function: "Imply(word_presences.is_present, titles.has_extractions)"
//         wordFactor.weight: "?(word_presences.word)"
//       }

//     """
//   }

//   it("should work") {
//     prepareData()
//     val config = ConfigFactory.parseString(getConfig).withFallback(ConfigFactory.load)
//     DeepDive.run(config, "out/test_lr")
//     // Make sure the data is in the database
//     JdbcDataStore.init(ConfigFactory.load)
//     PostgresDataStore.withConnection { implicit conn =>
     
//       val extractionResult = SQL("SELECT * FROM word_presences;")().map { row =>
//        row[Long]("id")
//       }.toList
//       assert(extractionResult.size == 12)
      
//       val numWeights = SQL("select count(*) as c from dd_graph_weights;")().head[Long]("c")

//       // Different weight for each unique word
//       assert(numWeights == 7)


//     }
//     JdbcDataStore.close()
//   }


// }