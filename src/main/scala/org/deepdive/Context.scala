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

  // The akka actor is initialized when "Context.system" is first accessed.
  // TODO: it might not be best to use a lazy val here, since we may want 
  // to run "DeepDive.run" multiple times, e.g. in tests.
  /* Notes @zifei:
    The difference between lazy val and val is, that a val is executed
    when it is defined, and a lazy val is executed when it is accessed the
    first time.

    We had encountered some problems when executing DeepDive.run several
    times in integration tests. We have been using some hacks to fix it
    (running these tests one by one in separate sbt commands). If we can
    fix it, we can run all tests together.

    I have to investigate more into the alternative. A possible way might
    be initializing Context.system explicitly every time DeepDive.run
    executes (not sure how), or making Context a class rather than an
    object. But I am not sure.
  */
  lazy val system = ActorSystem("deepdive")
  var outputDir = "out"
  // This needs to be variable since we might reassign it in relearnFrom feature
  
  // Set deepdive home according to environment variable. If not specified, 
  // use user's current directory
  val deepdiveHome = System.getenv("DEEPDIVE_HOME") match {
    case null => System.getProperty("user.dir")
    case _ => System.getenv("DEEPDIVE_HOME")
  }
  

  def shutdown(exitValue: Int = 0) {
    JdbcDataStore.close()
    system.shutdown()
    system.awaitTermination()
  }

}