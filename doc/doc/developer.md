---
layout: default
---

# Developer Guide & System Architecture 

DeepDive is written using the following frameworks and libraries:

- [Scala (2.10)](http://www.scala-lang.org/) 
- [Akka](http://akka.io/) for concurrent programming using actors
- [Scalatest](http://www.scalatest.org/) (and Akka's Testkit) for unit- and integration testing
- [RxScala](http://rxscala.github.io/) and [RxJava](https://github.com/Netflix/RxJava) for Observables
- [Anorm](http://www.playframework.com/documentation/2.2.1/ScalaAnorm) for RDBMS access
- [SBT](http://www.scala-sbt.org/) for dependency management

### Style Guide

Please follow the [Scala Style Guide](http://docs.scala-lang.org/style/) when contributing.

### Compiling 

You must install [SBT](http://www.scala-sbt.org/) to compile DeepDive. On Mac OS X, you can install SBT using [Homebrew](http://brew.sh/). You can then compile DeepDive using the following command in the root directory. SBT should download all required dependencies (including Scala) for you.

    sbt compile

During development we recommend running `sbt ~compile` instead. The tilde will automatically watch the directory for changes, and recompile modified files.


### Running Tests

DeepDive comes with a number of unit- and integration test. For tests that require connection to a database (such as PostgreSQL), you must modify the `src/test/recources/application.conf` file with your connection parameters. You can run all tests with:

    sbt test

To run a specific test, you can use:

    sbt "test-only org.deepdive.test.unit.[TestClassName]"

You can use `sbt ~test` to keep running tests in the background while you are modifying files.


### Reference: Factor Graph Output Schema

DeepDive uses a custom binary format to encode the factor graph. It generates four files: One for weights, variables, factors, and edges. All of the files can be found in the out/ directory of the latest run. The format of these files is as follows (numbers are in bytes):

Weights: 

    weightId        long    8
    isFixed         bool    1
    initialValue    double  8


Variables:

    variableId      long    8
    isEvidence      bool    1
    initialValue    double  8
    dataType        short   2
    edgeCount*      long    8
    cardinality     long    8

Factors

    factorId        long    8
    weightId        long    8
    factorFunction  short   2
    edgeCount       long    8

Edges

    variableId      long    8
    factorId        long    8
    position        long    8
    isPositive      bool    1
    equalPredicate  long    8


\*Note: from [version 0.03](changelog/0.03-alpha.html), the edgeCount field in Variables is always -1.

The systems also generates a metadata file of the following. The metadata file contains one comma-separated line with the following fields:

    Number of weights
    Number of variables
    Number of factors
    Number of edges
    Path to weights file
    Path to variables file
    Path to factors file
    Path to edges file


