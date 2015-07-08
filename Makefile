# Makefile for DeepDive

.DEFAULT_GOAL := install


### dependency recipes ########################################################

.PHONY: depends
depends:
	# Installing and Checking dependencies...
	util/install.sh deepdive_build_deps deepdive_runtime_deps
	lib/check-depends.sh


### install recipes ###########################################################

.PHONY: install
PREFIX = ~/local
DEST = $(PREFIX)/bin
install: build
	# Installing DeepDive to $(PREFIX)/
	mkdir -p $(DEST)
	ln -sfnv $(realpath shell/deepdive) $(DEST)/
	@if [ -x $(DEST)/deepdive ]; then \
		echo '# DeepDive binary has been install to $(DEST)/.'; \
		echo '# Make sure your shell is configured to include the directory in PATH environment, e.g.:'; \
		echo '  PATH=$(DEST):$$PATH'; \
	fi


### build recipes #############################################################

.PHONY: build
build: scala-build lib/dw

lib/dw:
	# Extracting sampler library
	lib/dw_extract.sh

# Scala-specific build and test recipes #######################################

# Why do we use Makefile instead of SBT?
# - Makefile can build components written in any language.  We use it anyway
#   for handling dependency and installation.
# - It's much easier to run integration tests with non-Scala components outside
#   SBT or ScalaTest.
# - SBT is slow.
#
# How can Makefile build and test Scala code?
# - By exporting the CLASSPATH and running java directly instead, we can avoid
#   SBT's poor command-line interface performance.  Note that we try to invoke
#   SBT as least as possible through out the Makefile, duplicating some commands.
# - Since there are test-specific classes and dependencies, there has to be two
#   CLASSPATHs.
# - When built for tests, the classes are instrumented such that coverage can
#   be measured.

# Some path names for Scala
SCALA_BUILD_FILES             = build.sbt $(wildcard project/*.*)
SCALA_MAIN_SOURCES            = $(shell find src/main/scala -name '*.scala')
SCALA_MAIN_CLASSES_DIR        = target/scala-2.10/classes
SCALA_MAIN_CLASSPATH_EXPORTED = target/scala-2.10/classpath
SCALA_TEST_SOURCES            = $(shell find src/test/scala -name '*.scala')
SCALA_TEST_CLASSES_DIR        = target/scala-2.10/test-classes
SCALA_TEST_CLASSPATH_EXPORTED = target/scala-2.10/test-classpath
SCALA_COVERAGE_DIR            = target/scala-2.10/scoverage-data

# SBT settings
PATH := $(PATH):$(shell pwd)/sbt
SBT_OPTS ?= -Xmx4g -XX:MaxHeapSize=4g -XX:MaxPermSize=4g
export SBT_OPTS

.PHONY: scala-build
scala-build: $(SCALA_MAIN_CLASSES_DIR) $(SCALA_MAIN_CLASSPATH_EXPORTED)
# How to build main Scala code and export main CLASSPATH
$(SCALA_MAIN_CLASSES_DIR): $(SCALA_MAIN_SOURCES) $(SCALA_BUILD_FILES)
	# Compiling Scala code
	sbt compile
	touch $(SCALA_MAIN_CLASSES_DIR)
$(SCALA_MAIN_CLASSPATH_EXPORTED): $(SCALA_BUILD_FILES)
	# Exporting CLASSPATH
	sbt --error "export compile:full-classpath" | tee /dev/stderr | \
	    tail -1 >$@

# How to build test Scala code with coverage and export test CLASSPATH
test-build: $(SCALA_COVERAGE_DIR) $(SCALA_TEST_CLASSES_DIR) $(SCALA_TEST_CLASSPATH_EXPORTED) lib/dw
$(SCALA_COVERAGE_DIR) $(SCALA_TEST_CLASSES_DIR): $(SCALA_MAIN_SOURCES) $(SCALA_TEST_SOURCES) $(SCALA_BUILD_FILES)
	# Compiling Scala code for test with coverage
	sbt coverage compile test:compile
	touch $(SCALA_COVERAGE_DIR) $(SCALA_TEST_CLASSES_DIR)
$(SCALA_TEST_CLASSPATH_EXPORTED): $(SCALA_BUILD_FILES)
	# Exporting CLASSPATH for tests
	sbt --error coverage "export test:full-classpath" | tee /dev/stderr | \
	    tail -1 | tee $(SCALA_MAIN_CLASSPATH_EXPORTED) >$@


### test recipes #############################################################

.PHONY: test test-build
# One can fine-tune the list of tests by setting environment variables
# TEST_ONLY and TEST_EXCEPT, or passing the same glob patterns as Make
# arguments ONLY and EXCEPT, e.g.:
#   $ make test ONLY+=test/postgres/*.bats EXCEPT+=test/*/scalatests.bats
#   $ TEST_ONLY=test/postgres/*.bats  make test EXCEPT+=test/*/scalatests.bats
#   $ TEST_ONLY=test/postgres/*.bats TEST_EXCEPT=test/*/scalatests.bats   make test
TEST_ONLY ?= $(shell test/enumerate-tests.sh)
TEST_EXCEPT ?=
test: ONLY = $(TEST_ONLY)
test: EXCEPT = $(TEST_EXCEPT)
test: BATS_FILES = $(filter-out $(wildcard $(EXCEPT)),$(wildcard $(ONLY)))
test: test/bats/bin/bats $(BATS_FILES) test-build
	# Running $(shell test/bats/bin/bats -c $(BATS_FILES)) tests defined in $(words $(BATS_FILES)) .bats files
	#  To test selectively, run:  make test   ONLY+=/path/to/bats/files
	#  To exclude certain tests:  make test EXCEPT+=/path/to/bats/files
	#  For a list of tests, run:  make test-list
	test/bats/bin/bats  $(BATS_FILES)
test-build:
test/bats/bin/bats:
	git submodule update --init test/bats
test/%/scalatests.bats: test/postgresql/update-scalatests.bats.sh $(SCALA_TEST_SOURCES)
	# Regenerating .bats for Scala tests
	$< >$@
	chmod +x $@

.PHONY: test-list
test-list:
	@echo "make test \\"
	@test/enumerate-tests.sh | sed 's/$$/ \\/; s/^/   ONLY+=/p; s/^   ONLY+=/ EXCEPT+=/'
	@echo " #"

.PHONY: checkstyle
checkstyle:
	@./test/checkstyle.sh


### submodule build recipes ###################################################

.PHONY: build-sampler
build-sampler:
	git submodule update --init sampler
	[ -e sampler/lib/gtest -a -e sampler/lib/tclap ] || $(MAKE) -C sampler dep
	$(MAKE) -C sampler dw
ifeq ($(shell uname),Linux)
	cp -f sampler/dw util/sampler-dw-linux
endif
ifeq ($(shell uname),Darwin)
	cp -f sampler/dw util/sampler-dw-mac
endif

.PHONY: build-mindbender
build-mindbender:
	git submodule update --init mindbender
	$(MAKE) -C mindbender
	cp -f mindbender/mindbender-LATEST-*.sh util/mindbender

.PHONY: build-ddlog
build-ddlog:
	git submodule update --init ddlog
	$(MAKE) -C ddlog ddlog.jar
	cp -f ddlog/ddlog.jar util/ddlog.jar

