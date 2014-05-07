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

  var samplerCmd = "util/sampler-dw-mac gibbs"
  val osname = System.getProperty("os.name")
  if (osname.startsWith("Linux")) {
    samplerCmd = "util/sampler-dw-linux gibbs"
  }

  // Still using old sampler command
  // describe("The Sampler") {
    
  //   it("should work with a trivial factor graph") {
  //     val sampler = TestActorRef[Sampler]
  //     //original sampler..
  //     val samplerCmd = "java -Xmx4g -jar util/sampler-assembly-0.1.jar"
  //     val samplerOptions = "-l 10 -s 10 -i 10"
  //     // val variablesOutputFile = File.createTempFile("sampler_output", "")
  //     sampler ! Sampler.Run(samplerCmd, samplerOptions, weightsFile, variablesFile, factorsFile, edgesFile,
  //       metaFile, "/tmp")
  //     expectMsg(Success())
  //   }

  //   it("should throw an exception when sampling fails") {
  //     val sampler = TestActorRef[Sampler]
  //     watch(sampler)
  //     //original sampler..
  //     val samplerCmd = "java -Xmx4g -jar util/sampler-assembly-0.1.jar"
  //     val samplerOptions = "-l 10 -s 10 -i 10"
  //     val variablesOutputFile = File.createTempFile("sampler_output", "")
  //     intercept[RuntimeException] {
  //       sampler.receive(Sampler.Run(samplerCmd, samplerOptions, "DOES_NOT_EXIST_FILE", variablesFile,
  //         factorsFile, edgesFile, metaFile, "/tmp"))
  //     }
  //   }

  // }

}

