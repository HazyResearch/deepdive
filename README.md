DeepDiveLog [![Build Status](https://travis-ci.org/HazyResearch/ddlog.svg)](https://travis-ci.org/HazyResearch/ddlog) [![Coverage Status](https://coveralls.io/repos/HazyResearch/ddlog/badge.svg)](https://coveralls.io/r/HazyResearch/ddlog)
===================

A Datalog-like language for writing DeepDive apps.

## Building
The following command builds all Scala code under src/.

```bash
make
```


## Testing

### Interactive Tests
To play with DDlog compiler's command-line interface interactively, use the following script after running `make`.

```bash
test/ddlog
```

This provides a quick way to see the effects from the command-line after changing the source code as it directly runs the compiled classes under target/ rather than having to assemble a jar every time.
After running `make` at least once, you can solely rely on an IDE such as [IntelliJ IDEA][] to compile individual .scala files and test it with this script.

### Automated Tests
The following command runs all tests under test/.
Tests are written in [BATS][].

```bash
make test
```

To run tests selectively, see the long make command produced by:

```bash
make test-list
```

### Test Coverage
The following command produces a test coverage report.

```bash
make test-coverage
```


## Deploying

### Assembling a standalone jar
The following command produces a standalone jar that contains the DDlog compiler.

```bash
make ddlog.jar
```

### Running the standalone jar
The following command generates a deepdive.conf for the [spouse example in DeepDive's tutorial](http://deepdive.stanford.edu/doc/basics/walkthrough/walkthrough.html).

```bash
mkdir -p examples/spouse_example
java -jar ddlog.jar compile examples/spouse_example.ddl >examples/spouse_example/deepdive.conf
```



[BATS]: https://github.com/sstephenson/bats#readme "Bash Automated Testing System"
[IntelliJ IDEA]: https://www.jetbrains.com/idea/
