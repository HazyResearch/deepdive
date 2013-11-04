import org.scalatest._
import org.deepdive.context.parsing.FactorFunctionParser
import org.deepdive._

class FactorFunctionParserSpec extends FunSpec {

  describe("The Factor function parser") {

    it("should parse empty imply expressions") {
      val expr = "Imply()"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(List()))
    }

    it("should parse imply expressions with multiple arguments") {
      val expr = "Imply(a,b)"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(result.successful)
      assert(result.get == ImplyFactorFunction(List("a","b")))
    }

    it("should not parse malformed Imply expressions") {
      val expr = "Imply(a"
      val result = FactorFunctionParser.parse(FactorFunctionParser.factorFunc, expr)
      assert(!result.successful)
    }

  }

}