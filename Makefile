# Makefile for DeepDiveLogCompiler

SOURCE_DIR = src/main/scala/org/deepdive/ddlog
TARGET_DIR = target/scala-2.10/classes
TEST_CLASSPATH_CACHE = $(TARGET_DIR)/dependency-classpath
JAR = ddlog.jar

# test
.PHONY: test
test: $(TARGET_DIR) $(TEST_CLASSPATH_CACHE)
	CLASSPATH=$(realpath $<):$(shell cat $(TEST_CLASSPATH_CACHE)) \
TEST_CLASS_OR_JAR=org.deepdive.ddlog.DeepDiveLog \
test/test.sh
$(TEST_CLASSPATH_CACHE): build.sbt $(wildcard project/*.sbt)
	sbt "export compile:dependency-classpath" | tail -1 >$@
$(TARGET_DIR): $(wildcard $(SOURCE_DIR)/*.scala)
	sbt compile

# test standalone package
.PHONY: test-package
test-package: $(JAR)
	CLASSPATH= \
TEST_CLASS_OR_JAR=$(realpath $(JAR)) \
test/test.sh

# build standalone jar
$(JAR): $(wildcard $(SOURCE_DIR)/*.scala)
	sbt clean assembly
	ln -sfn $$(ls -t target/scala-*/*-assembly-*.jar | head -1) $@
	touch $@

.PHONY: clean
clean:
	sbt clean
	# clean test artifacts
	rm -f $(JAR) $(TEST_CLASSPATH_CACHE) $(wildcard test/*/*/*.actual)
	find test/ -name '*.bats' -type l -exec rm -f {} +
