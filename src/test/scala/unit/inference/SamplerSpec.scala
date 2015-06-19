package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import java.io.File
import org.deepdive.inference.Sampler
import org.scalatest._
import scala.util.{Success, Failure}

class SamplerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSpecLike {

  def this() = this(ActorSystem("SamplerSpec"))

  val weightsFile = getClass.getResource("/inference/binary_factor_graph/weights").getFile()
  val variablesFile = getClass.getResource("/inference/binary_factor_graph/variables").getFile()
  val factorsFile = getClass.getResource("/inference/binary_factor_graph/factors").getFile()
  val edgesFile = getClass.getResource("/inference/binary_factor_graph/edges").getFile()
  val metaFile = getClass.getResource("/inference/binary_factor_graph/meta.csv").getFile()

  var samplerCmd = "util/sampler-dw"
}

