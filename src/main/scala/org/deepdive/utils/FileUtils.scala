package org.deepdive.utils

import java.nio.file._
import java.nio.file.attribute._
import java.nio.file.FileVisitResult._
import scala.collection.JavaConversions._
import scala.collection.mutable.{Set => MutableSet}
import java.io.{IOException, File}
import java.util.EnumSet

object FileUtils {

  class SetFileVisitor(matcher: PathMatcher) extends SimpleFileVisitor[Path] {
    val paths = MutableSet[String]()
    override def visitFile(file: Path, attrs: BasicFileAttributes) = {
      if (matcher.matches(file)) {
        paths += file.toFile.getCanonicalPath
      }
      FileVisitResult.CONTINUE;
    }
    override def visitFileFailed(file: Path, exception: IOException) = FileVisitResult.CONTINUE;
  }

  def glob(globPattern: String) : Set[String] = {
    val matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern)
    val visitor = new SetFileVisitor(matcher)
    Files.walkFileTree(Paths.get("."), visitor)
    visitor.paths.toSet
  }

  def absoluteFileOrGlob(fileStr: String) : Set[String] = {
    val file = new File(fileStr)
    if (file.exists && file.isAbsolute)
      return Set(file.getCanonicalPath)
    else
      return glob(fileStr)
  }


}