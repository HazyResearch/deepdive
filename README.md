DeepDiveLogCompiler
===================

A compiler that enables writing DeepDive apps in a Datalog-like syntax.

## Testing

```bash
make
```

## Building
The following command produces a standalone jar.

```bash
make ddlog.jar
```

## Running
The following will generate an application.conf for the [spouse example in DeepDive's tutorial](http://deepdive.stanford.edu/doc/basics/walkthrough/walkthrough.html).
```bash
mkdir -p examples/spouse_example
java -jar ddlog.jar examples/spouse_example.ddl >examples/spouse_example/application.conf
```

