package org.deepdive

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import java.io.File
import org.deepdive.datastore._
import org.deepdive.profiling.Profiler
import org.deepdive.settings._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

/* Describes the context of the DeepDive application */
object Context extends Logging {

  lazy val system = ActorSystem("deepdive")
  var outputDir = "out"
  
  // Set deepdive home according to environment variable. If not specified, 
  // use user's current directory
  val deepdiveHome = System.getenv("DEEPDIVE_HOME") match {
    case null => "."
    case _ => System.getenv("DEEPDIVE_HOME")
  }
  
//      System.getProperty("user.dir"))

  def shutdown(exitValue: Int = 0) {
    JdbcDataStore.close()
    system.shutdown()
    system.awaitTermination()
  }

}