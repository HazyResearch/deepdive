# Makefile for DeepDive

# install destination
PREFIX = ~/local
# path to the staging area
STAGE_DIR = dist/stage
# path to the area for keeping track of build
BUILD_DIR = .build
# path to the package to be built
PACKAGE = $(dir $(STAGE_DIR))deepdive.tar.gz

.DEFAULT_GOAL := build

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
build: inference/format_converter

# common build steps
BUILD_INFO=$(STAGE_DIR)/.build-info.sh
build:
	# staging all executable code and runtime data under $(STAGE_DIR)/
	./stage.sh $(STAGE_DIR)
	# record version and build info
	util/build/generate-build-info.sh >$(BUILD_INFO)

# how to build external runtime dependencies to bundle
.PHONY: extern/.build/bundled
extern/.build/bundled: extern/bundle-runtime-dependencies.sh
	PACKAGENAME=deepdive  $<
build: extern/.build/bundled


### test recipes #############################################################

# before testing on a clean source tree, make sure it's built at least once
test-build: $(BUILD_INFO)
$(BUILD_INFO):
	$(MAKE) build

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
build: build-dimmwitted

.PHONY: build-hocon2json
build-hocon2json:
	@util/build/build-submodule-if-needed compiler/hocon2json hocon2json.sh target/scala-2.10/hocon2json-assembly-0.1-SNAPSHOT.jar
build: build-hocon2json

.PHONY: build-mindbender
build-mindbender:
	@util/build/build-submodule-if-needed util/mindbender @prefix@/
build: build-mindbender

.PHONY: build-ddlog
build-ddlog:
	@util/build/build-submodule-if-needed compiler/ddlog target/scala-2.10/ddlog-assembly-0.1-SNAPSHOT.jar
build: build-ddlog

.PHONY: build-mkmimo
build-mkmimo:
	@util/build/build-submodule-if-needed runner/mkmimo mkmimo
build: build-mkmimo
