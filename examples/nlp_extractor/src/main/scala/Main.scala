package org.deepdive.udf.nlp

import play.api.libs.json._
import scala.io.Source
import java.util.Properties

object Main extends App {

  // Parse command line options
  case class Config(documentKey: String, idKey: String, maxSentenceLength: String, numThreads: String)

  val parser = new scopt.OptionParser[Config]("DeepDive DocumentParser") {
    head("documentParser", "0.1")
    opt[String]('v', "valueKey") required() action { (x, c) =>
      c.copy(documentKey = x) 
    } text("JSON key that contains the document, for example \"documents.text\"")
    opt[String]('k', "idKey") required() action { (x, c) =>
      c.copy(idKey = x) 
    } text("JSON key that contains the document id, for example \"documents.id\"")
    opt[String]('l', "maxLength") action { (x, c) =>
      c.copy(maxSentenceLength = x) 
    } text("Maximum length of sentences to parse (makes things faster) (default: 40)")
    opt[String]('t', "numThreads") action { (x, c) =>
      c.copy(numThreads = x) 
    } text("Number of threads (default: # of available cores)")
  }

  val conf = parser.parse(args, Config("documents.text", "documents.id", "40", 
      Runtime.getRuntime.availableProcessors.toString)) getOrElse { 
    throw new IllegalArgumentException
  }

  System.err.println(s"""Parsing with id_key="${conf.idKey}" value_key="${conf.documentKey}" """ +
    s"max_len=${conf.maxSentenceLength} numThreads=${conf.numThreads}")

  // Configuration has been parsed, execute the Document parser
  val props = new Properties()
  // props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, ner, parse, dcoref")
  props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, ner, parse")
  props.put("parse.maxlen", conf.maxSentenceLength)
  props.put("threads", conf.numThreads)
  val dp = new DocumentParser(props)

  // Read each json object from stdin and parse the document
  Source.stdin.getLines.zipWithIndex.foreach { case(line, idx) =>

    val jsObj = Json.parse(line).asInstanceOf[JsObject]
    
    val documentId = jsObj.value.get(conf.idKey)
    val documentStr = jsObj.value.get(conf.documentKey).map(_.asInstanceOf[JsString].value)

    System.err.println(s"Parsing document ${documentId.getOrElse("")}...")

    var sentence_offset = 0

    // Output a JSON tuple for each sentence
    documentStr.map(dp.parseDocumentString).map(_.sentences).getOrElse(Nil).foreach { sentenceResult =>
      //Console.println(sentenceResult.sentence)

      // Assign sentence_id as "docid@offset"
      val sentence_id = documentId.getOrElse(null) match {
          case null => JsNull
          case _ => JsString(s"${documentId.getOrElse("")}@${sentence_offset}")
        }

      val json = JsObject(Map(
        "document_id" -> documentId.getOrElse(JsNull),
        "sentence" -> JsString(sentenceResult.sentence),
        "wordidxs" -> JsArray(List.range(0,sentenceResult.words.length).map( i => JsNumber.apply(i))),
        "words" -> JsArray(sentenceResult.words.map(JsString.apply)),
        "pos_tags" -> JsArray(sentenceResult.wordsWithPos.map(JsString.apply)),
        "lemma" -> JsArray(sentenceResult.lemma.map(JsString.apply)),
        "dep_paths" -> JsArray(sentenceResult.depLabels.map(JsString.apply)),
        "dep_parents" -> JsArray(sentenceResult.depParents.map(  i => JsNumber.apply(i))),
        "ner_tags" -> JsArray(sentenceResult.nerTags.map(JsString.apply)),
        "sentence_offset" -> JsNumber(sentence_offset),
        "sentence_id" -> sentence_id
      ).toSeq)
      Console.println(Json.stringify(json))
      sentence_offset += 1
    }  
  }

}
