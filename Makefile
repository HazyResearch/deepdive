# Makefile for DDlog compiler


### Build & Clean #############################################################

.PHONY: build
build: scala-build

# build a standalone jar
JAR = ddlog.jar
.PHONY: package
package: $(JAR)
$(JAR): scala-assembly-jar
	ln -sfn $(SCALA_ASSEMBLY_JAR) $@
	touch $@

.PHONY: clean
clean: scala-clean
	# clean test artifacts
	rm -f $(JAR) $(wildcard test/*/*/*.actual)
	find test/ -name '*.bats' -type l -exec rm -f {} +

PATH := $(shell pwd)/project/sbt:$(PATH)
export PATH
ifeq ($(shell uname),Darwin)
SHELL := /bin/bash  # Mac requires SHELL to be forced to bash
endif

include scala.mk  # defines scala-build, scala-test-build, scala-assembly-jar, scala-clean targets



### Test ######################################################################

# test prep for built classes or assembly jar
test-package: $(JAR)
	$(MAKE) test TEST_JAR=1
ifndef TEST_JAR
test-build: scala-test-build
test: export CLASSPATH = $(shell cat $(SCALA_TEST_CLASSPATH_EXPORTED))

else # ifdef TEST_JAR
.PHONY: test-package
test-build: $(JAR)
test: export CLASSPATH = $(realpath $(JAR))

endif # TEST_JAR

actual-expected:
	for expected in test/*/*/*.expected; do \
	    case $$expected in \
	        *-error.expected) actual=$${expected%-error.expected}.actual;; \
	        *)                actual=$${expected%.expected}.actual      ;; \
	    esac; \
	    [[ "$$actual" -nt "$$expected" ]] && \
	    ! diff -q --ignore-all-space "$$actual" "$$expected" || continue; \
	    cp -vf "$$actual" "$$expected"; \
	done

# test coverage report
.PHONY: test-coverage coveralls
test-coverage:
	-$(MAKE) test
	sbt coverageReport
coveralls: test-coverage
	# submit coverage data to https://coveralls.io/r/HazyResearch/ddlog
	# (Make sure you have set COVERALLS_REPO_TOKEN=...)
	sbt coveralls

include test/bats.mk  # defines test, test-build, test-list targets
