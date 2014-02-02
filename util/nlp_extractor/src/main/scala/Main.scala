package org.deepdive.udf.nlp

import play.api.libs.json._
import scala.io.Source
import java.util.Properties

object Main extends App {

  // Parse command line options
  case class Config(documentKey: String, idKey: String, maxSentenceLength: Int)

  val parser = new scopt.OptionParser[Config]("DeepDive DocumentParser") {
    head("documentParser", "0.1")
    opt[String]('v', "valueKey") required() action { (x, c) =>
      c.copy(documentKey = x) 
    } text("JSON key that contains the document, for example \"documents.text\"")
    opt[String]('k', "idKey") required() action { (x, c) =>
      c.copy(idKey = x) 
    } text("JSON key that contains the document id, for example \"documents.id\"")
    opt[Int]('l', "maxLength") action { (x, c) =>
      c.copy(maxSentenceLength = x) 
    } text("Maximum length of sentences to parse (makes things faster) (default: 40)")
  }

  val conf = parser.parse(args, Config("documents.text", "documents.id", 40)) getOrElse { 
    throw new IllegalArgumentException
  }

  // Configuration has been parsed, execute the Document parser
  val props = new Properties()
  props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, ner, parse, dcoref")
  props.put("parse.maxlen", conf.maxSentenceLength.asInstanceOf[java.lang.Object])
  props.put("pos.maxlen", conf.maxSentenceLength.asInstanceOf[java.lang.Object])
  val dp = new DocumentParser(props)

  // Read each json object from stdin and parse the document
  Source.stdin.getLines.foreach { line =>
    val jsObj = Json.parse(line).asInstanceOf[JsObject]
    
    val documentId = jsObj.value.get(conf.idKey)
    val documentStr = jsObj.value.get(conf.documentKey).map(_.asInstanceOf[JsString].value)

    // Output a JSON tuple for each sentence
    documentStr.map(dp.parseDocumentString).map(_.sentences).getOrElse(Nil).foreach { sentenceResult =>
      //Console.println(sentenceResult.sentence)
      val json = JsObject(Map(
        "document_id" -> documentId.getOrElse(JsNull),
        "sentence" -> JsString(sentenceResult.sentence),
        "words" -> JsArray(sentenceResult.words.map(JsString.apply)),
        "pos_tags" -> JsArray(sentenceResult.wordsWithPos.map(JsString.apply)),
        "dependencies" -> JsArray(sentenceResult.deps.map(JsString.apply)),
        "ner_tags" -> JsArray(sentenceResult.nerTags.map(JsString.apply))
      ).toSeq)
      Console.println(Json.stringify(json))
    }  
  }

}
