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

DeepDive comes with a number of unit- and integration test. For tests that require connection to a database (such as postgres), you must modify the `stc/test/recources/application.conf` file with your connection parameters. You can run all tests with:

    sbt test

To run a specific test, you can use:

    sbt "test-only org.deepdive.test.unit.[TestClassName]"

You can use `sbt ~test` to keep running tests in the backgroud while you are modifying files.



### Reference: Factor Graph Output Schema

The systems outputs three **space-separated** files for weights, factors and variables in the following format.

Weights file:

    [weight_id] [initial_value] [is_fixed] [description]
    1013 0.0 false words(words.word=Some(Kelly))
    585 0.0 false words(words.word=Some(James))

Factors file:

    [factor_id] [weight_id] [factor_function]
    251 187 ImplyFactorFunction
    2026 382 ImplyFactorFunction

Variables file:

    [variable_id] [factor_id] [position]  [is_positive] [data_type] [initial_value] [is_evidence] [is_query]
    0 0 1 true Discrete 0.0 true false
    1 1 1 true Discrete 0.0 true false
    2 2 1 true Discrete 0.0 true false

Note the the last file is effectively a map between variables and factors. This means that a variable id can appear multiple times. `data_type` `initial_value` `is_evidence` `is_query` are properties of a variable as opposed to properties the variable mapping, thus these are equal for a variable id and may be redundant.