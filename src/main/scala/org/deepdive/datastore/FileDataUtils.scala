package org.deepdive.datastore

import org.deepdive.Logging
import au.com.bytecode.opencsv.CSVReader
import scala.io.Source
import play.api.libs.json._
import scala.collection.JavaConversions._
import java.io.{ File, BufferedInputStream, FileInputStream, ByteArrayOutputStream }
import java.util.zip.{ ZipEntry, ZipOutputStream }


object FileDataUtils extends Logging {
  def queryAsJson[A](filename: String, sep: Char)(block: Iterator[JsValue] => A) : A = {
    val reader = new CSVReader(Source.fromFile(filename).reader, sep)
    try {
      // TODO: Don't read this into memory.
      val jsonData = reader.readAll.map(x => JsArray(x.map(JsString)))
      block(jsonData.iterator)
    } finally {
      reader.close()
    }
  }

  def recursiveListFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  def zipDir(dir: String): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(baos)
    val files = recursiveListFiles(new File(dir)).filter(!_.isDirectory)

    files.foreach { file =>
      val relativePath = new File(dir).toURI.relativize(file.toURI).getPath
      zip.putNextEntry(new ZipEntry(relativePath))
      val in = new BufferedInputStream(new FileInputStream(file), 1 << 20)
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }
    zip.close()
    return baos.toByteArray()
  }

}
