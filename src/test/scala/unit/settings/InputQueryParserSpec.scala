package org.deepdive.test.unit

import org.deepdive.settings._
import org.scalatest._

class InputQueryParserSpec extends FunSpec {

  describe("Parsing filename expressions") {
    
    it("should work with simple file names") {
      val expr = "'someFile.txt'"
      val result = InputQueryParser.parse(InputQueryParser.filenameExpr, expr)
      assert(result.successful)
      assert(result.get == "someFile.txt")
    }

    it("should work with file paths") {
      val expr = "'../path/to/someFile.txt'"
      val result = InputQueryParser.parse(InputQueryParser.filenameExpr, expr)
      assert(result.successful)
      assert(result.get == "../path/to/someFile.txt")
    }

  }

  describe("Parsing CSV input query expressions") {
    it("should work") {
      val expr = "CSV('someFile.txt')"
      val result = InputQueryParser.parse(InputQueryParser.inputQueryExpr, expr)
      assert(result.successful)
      assert(result.get == CSVInputQuery("someFile.txt", ','))
    }
    it("should not work on wrong CSV filename") {
      val expr = "CSV()"
      val result = InputQueryParser.parse(InputQueryParser.inputQueryExpr, expr)
      assert(!result.successful)
    }
  }

  describe("Parsing TSV input query expressions") {
    it("should work") {
      val expr = "TSV('someFile.txt')"
      val result = InputQueryParser.parse(InputQueryParser.inputQueryExpr, expr)
      assert(result.successful)
      assert(result.get == CSVInputQuery("someFile.txt", '\t'))
    }
    it("should not work on wrong TSV filename") {
      val expr = "TSV()"
      val result = InputQueryParser.parse(InputQueryParser.inputQueryExpr, expr)
      assert(!result.successful)
    }
  }

  describe("Parsing arbitary datastore input query expressions") {
    it("should work") {
      val expr = "SELECT * FROM Customers"
      val result = InputQueryParser.parse(InputQueryParser.inputQueryExpr, expr)
      assert(result.successful)
      assert(result.get == DatastoreInputQuery("SELECT * FROM Customers"))
    }

    it("should remove trailing semicolons") {
      val expr = "SELECT * FROM Customers;  "
      val result = InputQueryParser.parse(InputQueryParser.inputQueryExpr, expr)
      assert(result.successful)
      assert(result.get == DatastoreInputQuery("SELECT * FROM Customers"))
    }
  }

}