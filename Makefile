# Makefile for DeepDive

# install destination
PREFIX = ~/local
# path to the staging area
STAGE_DIR = dist/stage
# path to the package to be built
PACKAGE = $(dir $(STAGE_DIR))/deepdive.tar.gz

.DEFAULT_GOAL := install

### dependency recipes ########################################################

.PHONY: depends
depends:
	# Installing and Checking dependencies...
	util/install.sh deepdive_build_deps deepdive_runtime_deps


### install recipes ###########################################################

.PHONY: install
install: build
	# Installing DeepDive to $(PREFIX)/
	mkdir -p $(PREFIX)
	tar cf - -C $(STAGE_DIR) . | tar xf - -C $(PREFIX)
	# DeepDive has been install to $(PREFIX)/
	# Make sure your shell is configured to include the directory in PATH environment, e.g.:
	#    PATH=$(PREFIX)/bin:$$PATH

.PHONY: package
package: $(PACKAGE)
$(PACKAGE): build
	tar cf $@ -C $(STAGE_DIR) .

### build recipes #############################################################

.PHONY: build
export STAGE_DIR
# TODO record version

build: scala-assembly-jar
	# staging all executable code and runtime data under $(STAGE_DIR)/
	./stage.sh $(STAGE_DIR)
	echo 'export CLASSPATH="$$DEEPDIVE_HOME"/lib/deepdive.jar' >$(STAGE_DIR)/env.sh

test-build: scala-test-build
	# staging all executable code and runtime data under $(STAGE_DIR)/
	./stage.sh $(STAGE_DIR)
	echo "export CLASSPATH='$$(cat $(SCALA_TEST_CLASSPATH_EXPORTED))'" >$(STAGE_DIR)/env.sh

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

