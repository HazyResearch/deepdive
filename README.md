DeepDiveLogCompiler
===================

A compiler that enables writing DeepDive apps in a Datalog-like syntax.

## Building

```bash
make
```

## Running
The following will generate an application.conf for the [spouse example in DeepDive's tutorial](http://deepdive.stanford.edu/doc/basics/walkthrough/walkthrough.html).
```bash
scala ddlc.jar examples/spouse_example.ddl >application.conf
```
