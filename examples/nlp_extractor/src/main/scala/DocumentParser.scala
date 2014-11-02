package org.deepdive.udf.nlp

import edu.stanford.nlp.ling.{CoreLabel, Word, CoreAnnotations}
import edu.stanford.nlp.parser._
import edu.stanford.nlp.parser.lexparser._
import edu.stanford.nlp.process._
import edu.stanford.nlp.trees._
import edu.stanford.nlp.ie.crf._
import edu.stanford.nlp.pipeline._
import edu.stanford.nlp.util._
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation
// CollapsedCCProcessedDependenciesAnnotation
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation
import scala.collection.JavaConversions._
import java.io.{StringReader, StringWriter, PrintWriter}
import java.util.Properties


class DocumentParser(props: Properties) {

  val pipeline = new StanfordCoreNLP(props)

  def parseDocumentString(doc: String) = {

    val document = new Annotation(doc)
    pipeline.annotate(document)
    val dcoref = document.get(classOf[CorefChainAnnotation])
    val sentences = document.get(classOf[SentencesAnnotation])

    val sentenceResults = sentences.zipWithIndex.map { case(sentence, sentIdx) =>
      val tokens = sentence.get(classOf[TokensAnnotation])
      val wordList = tokens.map(_.get(classOf[TextAnnotation]))
      val posList = tokens.map(_.get(classOf[PartOfSpeechAnnotation]))
      val nerList = tokens.map(_.get(classOf[NamedEntityTagAnnotation]))
      val lemmaList = tokens.map(_.get(classOf[LemmaAnnotation]))
      val depList = sentence.get(classOf[CollapsedCCProcessedDependenciesAnnotation]).toList.lines
      // Example depList: 
      // ["root(ROOT-0, sentence-4)","nsubj(sentence-4, I-1)","cop(sentence-4, am-2)","det(sentence-4, another-3)","vmod(sentence-4, called-6)","dobj(called-6, sentence-7)","num(sentence-7, three-8)"]
      val dep_labels = Array.fill(wordList.length){"_"}
      val dep_parents = Array.fill(wordList.length){0}
      val regex = """(.*)\(.*-(\d+), .*-(\d+)\)""".r
      depList.foreach {
        case s =>
          s match {
            case regex(tag, parent, index) =>
              dep_labels(Integer.parseInt(index) - 1) = tag;
              dep_parents(Integer.parseInt(index) - 1) = Integer.parseInt(parent);
            case _ =>
          }
      }
      SentenceParseResult(wordList.mkString(" "), wordList.toList, lemmaList.toList,
        posList.toList, dep_labels.toList, dep_parents.toList, nerList.toList)
    }

    DocumentParseResult(sentenceResults.toList) 
  }



}