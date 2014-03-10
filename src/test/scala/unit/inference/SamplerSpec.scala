package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import java.io.File
import org.deepdive.inference.Sampler
import org.scalatest._
import scala.util.{Success, Failure}

class SamplerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSpecLike {

  def this() = this(ActorSystem("SamplerSpec"))

  val weightsFile = getClass.getResource("/inference/trivial_factor_graph/graph.weights.pb").getFile()
  val variablesFile = getClass.getResource("/inference/trivial_factor_graph/graph.variables.pb").getFile()
  val factorsFile = getClass.getResource("/inference/trivial_factor_graph/graph.factors.pb").getFile()
  val edgesFile = getClass.getResource("/inference/trivial_factor_graph/graph.edges.pb").getFile()
  val metaFile = getClass.getResource("/inference/trivial_factor_graph/graph.edges.pb").getFile()

  describe("The Sampler") {
    
    it("should work with a trivial factor graph") {
      val sampler = TestActorRef[Sampler]
      val samplerCmd = "LD_LIBRARY_PATH=/dfs/rulk/0/hazy_share/lib64/:/dfs/rulk/0/hazy_share/lib/protobuf/lib/:/dfs/rulk/0/hazy_share/lib/tclap/lib/ /dfs/rulk/0/hazy_share/./dw gibbs"
      val samplerOptions = "-l 10 -s 10 -i 10"
      // val variablesOutputFile = File.createTempFile("sampler_output", "")
      sampler ! Sampler.Run(samplerCmd, samplerOptions, weightsFile, variablesFile, factorsFile, edgesFile,
        metaFile, "/tmp")
      expectMsg(Success())
    }

    it("should throw an exception when sampling fails") {
      val sampler = TestActorRef[Sampler]
      watch(sampler)
      val samplerCmd = "LD_LIBRARY_PATH=/dfs/rulk/0/hazy_share/lib64/:/dfs/rulk/0/hazy_share/lib/protobuf/lib/:/dfs/rulk/0/hazy_share/lib/tclap/lib/ /dfs/rulk/0/hazy_share/./dw gibbs"
      val samplerOptions = "-l 10 -s 10 -i 10"
      val variablesOutputFile = File.createTempFile("sampler_output", "")
      intercept[RuntimeException] {
        sampler.receive(Sampler.Run(samplerCmd, samplerOptions, "DOES_NOT_EXIST_FILE", variablesFile,
          factorsFile, edgesFile, metaFile, "/tmp"))
      }
    }

  }

}

