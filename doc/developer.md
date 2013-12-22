# Developer Guide & System Architecture 

DeepDive is written in [Scala (2.10)](http://www.scala-lang.org/) and makes heavy use of the [Akka](http://akka.io/) framework. We use [Anorm](http://www.playframework.com/documentation/2.2.1/ScalaAnorm) for RDBMS access. Unit- and integration tests are written using [Scalatest](http://www.scalatest.org/). DeepDive uses [sbt](http://www.scala-sbt.org/) for dependency management.

Please follow the [Scala Style Guide](http://docs.scala-lang.org/style/) when contributing.

### Compiling 

    sbt ~compile

Note: The "~" means that sbt will watch files and automatically recompile when a file changes. Useful during development.

### Running Tests

    sbt ~test

### Factor Graph Database Schema


    weights(id, initial_value, is_fixed)
    factors(id, weight_id, factor_function)
    variables(id, data_type, initial_value, is_evidence, is_query)
    factor_variables(factor_id, variable_id, position, is_positive)

### Factor Graph Output Schema

The systems outputs three **tab-separated** files for weights, factors and variables, with the following schemas, respectively:

    weight_id initial_value is_fixed
    1013  0.0 false
    585 0.0 false

    factor_id weight_id factor_function
    251 187 ImplyFactorFunction
    2026  382 ImplyFactorFunction

    variable_id factor_id position  is_positive data_type initial_value is_evidence is_query
    0 0 1 true  Discrete  0.0 true  false
    1 1 1 true  Discrete  0.0 true  false
    2 2 1 true  Discrete  0.0 true  false

Note the the last file is effectively a map between variables and factors. This means that a variable id can appear multiple times. `data_type` `initial_value` `is_evidence` `is_query` are properties of a variable as opposed to properties the variable mapping, thus these are equal for a variable id and may be redundant.