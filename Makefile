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
test-build: scala-test-build lib/dw

include scala.mk  # for scala-build, scala-test-build, scala-assembly-jar, scala-clean, etc. targets


### test recipes #############################################################

test/%/scalatests.bats: test/postgresql/update-scalatests.bats.sh $(SCALA_TEST_SOURCES)
	# Regenerating .bats for Scala tests
	$< >$@
	chmod +x $@

include test/bats.mk

.PHONY: checkstyle
checkstyle:
	test/checkstyle.sh


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

