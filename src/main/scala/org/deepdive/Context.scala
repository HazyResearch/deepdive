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

/* Describes the context of the DeepDive application */
object Context extends Logging {

  lazy val system = ActorSystem("deepdive")
  var configFile : File = null
  var settings : Settings = null

  def report() {
    implicit val timeout = Timeout(5.seconds)
    Await.result(system.actorSelection("/user/profiler") ? Profiler.Report, 5.seconds)
  }

  def shutdown(exitValue: Int = 0) {
    report()
    log.info("shutting down")
    system.shutdown()
    system.awaitTermination()
    PostgresDataStore.close()
  }

}