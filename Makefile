# Makefile for DeepDive

# install destination
PREFIX = ~/local
# path to the staging area
STAGE_DIR = dist/stage
# path to the area for keeping track of build
BUILD_DIR = .build
# path to the package to be built
PACKAGE = $(dir $(STAGE_DIR))deepdive.tar.gz

.DEFAULT_GOAL := test-build

### dependency recipes ########################################################

.PHONY: depends
depends:
	# Installing and Checking dependencies...
	util/install.sh _deepdive_build_deps _deepdive_runtime_deps

### install recipes ###########################################################

.PHONY: install
install: build
	# Installing DeepDive to $(PREFIX)/
	mkdir -p $(PREFIX)
	tar cf - -C $(STAGE_DIR) . | tar xf - -C $(PREFIX)
	# DeepDive has been install to $(PREFIX)/
	# Make sure your shell is configured to include the directory in PATH environment, e.g.:
	#    export PATH="$(PREFIX)/bin:$$PATH"

.PHONY: package
package: $(PACKAGE)
$(PACKAGE): build
	tar czf $@ -C $(STAGE_DIR) .

### release recipes ###########################################################

# For example, `make release-v0.7.0` builds the package, tags it with version
# v0.7.0, then uploads the file to the GitHub release.  Installers under
# util/install/ can then download and install the binary release directly
# without building the source tree.

release-%: GITHUB_REPO = HazyResearch/deepdive
release-%: RELEASE_VERSION = $*
release-%: RELEASE_PACKAGE = deepdive-$(RELEASE_VERSION)-$(shell uname).tar.gz
release-%:
	git tag --annotate --force $(RELEASE_VERSION)
	$(MAKE) RELEASE_VERSION=$(RELEASE_VERSION) $(PACKAGE)
	ln -sfn $(PACKAGE) $(RELEASE_PACKAGE)
	# Releasing $(RELEASE_PACKAGE) to GitHub
	# (Make sure GITHUB_OAUTH_TOKEN is set directly or via ~/.netrc or OS X Keychain)
	util/build/upload-github-release-asset \
	    file=$(RELEASE_PACKAGE) \
	    repo=$(GITHUB_REPO) \
	    tag=$(RELEASE_VERSION)
	# Released $(RELEASE_PACKAGE) to GitHub

### build recipes #############################################################

# binary format converter for sampler
inference/format_converter: inference/format_converter.cc
	$(CXX) -Os -o $@ $^
test-build build: inference/format_converter


# common build steps between build and test-build targets
define STAGING_COMMANDS
	# staging all executable code and runtime data under $(STAGE_DIR)/
	./stage.sh $(STAGE_DIR)
	# record version and build info
	util/build/generate-build-info.sh >$(STAGE_DIR)/.build-info.sh
endef

.PHONY: build
build: scala-assembly-jar
	$(STAGING_COMMANDS)
	# record production environment settings
	echo 'export CLASSPATH="$$DEEPDIVE_HOME"/lib/deepdive.jar' >$(STAGE_DIR)/env.sh

test-build: scala-test-build
	$(STAGING_COMMANDS)
	# record test-specific environment settings
	echo "export CLASSPATH='$$(cat $(SCALA_TEST_CLASSPATH_EXPORTED))'" >$(STAGE_DIR)/env.sh

# use bundled SBT launcher when necessary
PATH += :$(shell pwd)/sbt
# XXX For some inexplicable reason on OS X, the default SHELL (/bin/sh) won't pickup the extended PATH, so overriding it to bash.
ifeq ($(shell uname),Darwin)
export SHELL := /bin/bash
endif

include scala.mk  # for scala-build, scala-test-build, scala-assembly-jar, scala-clean, etc. targets

# how to build runtime dependencies to bundle
.PHONY: depends/.build/bundled
depends/.build/bundled: depends/bundle-runtime-dependencies.sh
	PACKAGENAME=deepdive  $<
test-build build: depends/.build/bundled


### test recipes #############################################################

# make sure test is against the code built and staged by this Makefile
DEEPDIVE_HOME := $(realpath $(STAGE_DIR))
export DEEPDIVE_HOME

include test/bats.mk
TEST_LIST_COMMAND = mkdir -p $(STAGE_DIR) && $(TEST_ROOT)/enumerate-tests.sh

.PHONY: checkstyle
checkstyle:
	test/checkstyle.sh


### submodule build recipes ###################################################

.PHONY: build-sampler
build-sampler: build-dimmwitted
.PHONY: build-dimmwitted
build-dimmwitted:
	@util/build/build-submodule-if-needed inference/dimmwitted dw
test-build build: build-dimmwitted

.PHONY: build-hocon2json
build-hocon2json:
	@util/build/build-submodule-if-needed compiler/hocon2json hocon2json.sh target/scala-2.10/hocon2json-assembly-0.1-SNAPSHOT.jar
test-build build: build-hocon2json

.PHONY: build-mindbender
build-mindbender:
	@util/build/build-submodule-if-needed mindbender mindbender-LATEST.sh

.PHONY: build-ddlog
build-ddlog:
	@util/build/build-submodule-if-needed compiler/ddlog target/scala-2.10/ddlog-assembly-0.1-SNAPSHOT.jar
test-build build: build-ddlog

.PHONY: build-mkmimo
build-mkmimo:
	@util/build/build-submodule-if-needed runner/mkmimo mkmimo
test-build build: build-mkmimo
