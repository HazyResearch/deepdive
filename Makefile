# Makefile for DeepDiveLogCompiler

JAR = ddlog.jar
TEST_JAR = ddlog-test.jar
TEST_CLASSPATH_CACHE = ddlog-test.jar.classpath

# test
.PHONY: test
test: $(TEST_JAR) $(TEST_CLASSPATH_CACHE)
	DDLOG_JAR=$(realpath $<) \
CLASSPATH=$(shell cat $(TEST_CLASSPATH_CACHE)) \
test/test.sh
$(TEST_CLASSPATH_CACHE): build.sbt $(wildcard project/*.sbt)
	sbt "export compile:dependency-classpath" | tail -1 >$@

# test standalone package
.PHONY: test-package
test-package: $(JAR)
	$(MAKE) test TEST_JAR=$<

# build test jar
$(TEST_JAR): $(wildcard *.scala)
	sbt package
	ln -sfn $$(ls -t target/scala-*/*_*.jar | head -1) $@
	touch $@

# build standalone jar
$(JAR): $(wildcard *.scala)
	sbt assembly
	ln -sfn $$(ls -t target/scala-*/*-assembly-*.jar | head -1) $@
	touch $@

.PHONY: clean
clean:
	sbt clean
