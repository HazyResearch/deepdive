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
//        SQL("create table titles(id bigserial primary key, title text, has_extractions boolean);").execute()
//        SQL("""create table word_presences(id bigserial primary key, 
//         title_id bigint references titles(id), word text, is_present boolean);""").execute()
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
      
//       val numFactors = SQL("select count(*) as c from dd_graph_factors;")().head[Long]("c")
//       val numVariables = SQL("select count(*) as c from dd_graph_variables;")().head[Long]("c")
//       val numFactorVariables = SQL("select count(*) as c from dd_graph_edges;")().head[Long]("c")
//       val numWeights = SQL("select count(*) as c from dd_graph_weights;")().head[Long]("c")

//       // One variable for each word, and one variable for each title
//       assert(numVariables == 15)
//       // One factor for each word
//       assert(numFactors == 12)
//       // Each factor connects one word and one title (12*2)
//       assert(numFactorVariables == 24)
//       // Different weight for each unique word
//       assert(numWeights == 7)

//       // Make sure the variables types are correct
//       val numEvidence = SQL("""
//         select count(*) as c from dd_graph_variables 
//         WHERE is_evidence = true""")().head[Long]("c")
//       val numQuery = SQL("""
//         select count(*) as c from dd_graph_variables 
//         WHERE is_evidence = false""")().head[Long]("c")
//       // 1 title and 12 words are evidence
//       assert(numEvidence == 13)
//       assert(numQuery == 2)

//     }
//     JdbcDataStore.close()
//   }


// }