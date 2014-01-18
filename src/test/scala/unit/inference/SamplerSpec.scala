package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import java.io.File
import org.deepdive.inference.Sampler
import org.scalatest._
import scala.util.{Success, Failure}

class SamplerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSpecLike {

  def this() = this(ActorSystem("SamplerSpec"))

  val VariablesFile = getClass.getResource("/inference/trivial_factor_graph/variables.tsv").getFile()
  val FactorsFile = getClass.getResource("/inference/trivial_factor_graph/factors.tsv").getFile()
  val WeightsFile = getClass.getResource("/inference/trivial_factor_graph/weights.tsv").getFile()

  describe("The Sampler") {
    
    it("should work with a trivial factor graph") {
      val sampler = TestActorRef[Sampler]
      val javaArgs = "-Xmx4g"
      val samplerOptions = "-l 10 -s 10 -i 10"
      val variablesOutputFile = File.createTempFile("sampler_output", "")
      sampler ! Sampler.Run(javaArgs, samplerOptions, VariablesFile, 
        FactorsFile, WeightsFile, variablesOutputFile.getCanonicalPath)
      expectMsg(Success())
    }

    it("should throw an exception when sampling fails") {
      val sampler = TestActorRef[Sampler]
      watch(sampler)
      val javaArgs = "-Xmx4g"
      val samplerOptions = "-l 10 -s 10 -i 10"
      val variablesOutputFile = File.createTempFile("sampler_output", "")
      intercept[RuntimeException] {
        sampler.receive(Sampler.Run(javaArgs, samplerOptions, VariablesFile, 
        FactorsFile, "DOES_NOT_EXIST_FILE", variablesOutputFile.getCanonicalPath))
      }
    }

  }

}

