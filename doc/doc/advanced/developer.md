---
layout: default
---

# Scala Developer Guide for DeepDive

DeepDive is written using the following frameworks and libraries:

- [Scala (2.10)](http://www.scala-lang.org/) 
- [Akka](http://akka.io/) for concurrent programming using actors
- [Scalatest](http://www.scalatest.org/) (and Akka's Testkit) for unit- and integration testing
- [RxScala](http://rxscala.github.io/) and [RxJava](https://github.com/Netflix/RxJava) for Observables
- [Anorm](http://www.playframework.com/documentation/2.2.1/ScalaAnorm) for RDBMS access
- [SBT](http://www.scala-sbt.org/) for dependency management

You only need to install SBT. The rest of the dependences will be installed by
DeepDive during the first compilation.

### Compiling 

You must install [SBT](http://www.scala-sbt.org/) to compile DeepDive. On Mac OS
X, you can install SBT using [Homebrew](http://brew.sh/. To compile DeepDive,
run the following command from the `deepdive/` directory: 

```bash
sbt compile
```

SBT should download all required dependencies (including Scala) for you.

During development we recommend running `sbt ~compile` instead. The tilde will
automatically watch the directory for changes, and recompile only modified files.

### Running Tests

DeepDive comes with a number of unit- and integration tests. For tests that
require connecting to a database (such as PostgreSQL), you must modify the
`src/test/recources/application.conf` file with your connection parameters. You
can run all tests with:

```bash
sbt test
```

To run a specific test, you can use:

```bash
sbt "test-only org.deepdive.test.unit.[TestClassName]"
```

You can use `sbt ~test` to keep running tests in the background while modifying
files.

### Style Guide

Please follow the [Scala Style Guide](http://docs.scala-lang.org/style/) when
contributing.

