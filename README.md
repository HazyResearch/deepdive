DeepDiveLog [![Build Status](https://travis-ci.org/HazyResearch/ddlog.svg)](https://travis-ci.org/HazyResearch/ddlog) [![Coverage Status](https://coveralls.io/repos/HazyResearch/ddlog/badge.svg)](https://coveralls.io/r/HazyResearch/ddlog)
===================

A Datalog-like language for writing DeepDive apps.

## Building
The following command produces a standalone jar that contains the compiler.

```bash
make ddlog.jar
```

## Running
The following will generate an application.conf for the [spouse example in DeepDive's tutorial](http://deepdive.stanford.edu/doc/basics/walkthrough/walkthrough.html).

```bash
mkdir -p examples/spouse_example
java -jar ddlog.jar compile examples/spouse_example.ddl >examples/spouse_example/application.conf
```


## Testing
The following command runs all tests under test/.

```bash
make  # or make test
```

## Coverage
The following command produces a test coverage report.

```bash
make test-coverage
```
