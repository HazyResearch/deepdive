# Makefile for DeepDiveLogCompiler

TESTJAR = ddlc-test.jar
TEST = examples/spouse_example.ddl

test: $(TESTJAR)
	CLASSPATH=$(shell sbt "export compile:dependency-classpath" | tail -1) \
	scala $< $(TEST) | diff -u $(TEST:.ddl=.expected) -
$(TESTJAR): $(wildcard *.scala)
	sbt package
	ln -sfn $(shell ls -t target/scala-*/*_*.jar | head -1) $@
	touch $@

# standalone jar
JAR = ddlc.jar
test-package: $(JAR)
	scala $< $(TEST) | diff -u $(TEST:.ddl=.expected) -
$(JAR): $(wildcard *.scala)
	sbt assembly
	ln -sfn $(shell ls -t target/scala-*/*-assembly-*.jar | head -1) $@
	touch $@

clean:
	sbt clean
