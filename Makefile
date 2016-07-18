# Makefile for DeepDive

# install destination
PREFIX = ~/local
# path to the staging area
STAGE_DIR = dist/stage
# path to the area for keeping track of build
BUILD_DIR = .build
# path to the package to be built
PACKAGE = $(dir $(STAGE_DIR))deepdive.tar.gz

ifneq ($(shell which docker 2>/dev/null),)
# do a containerized build by default if Docker is available
.DEFAULT_GOAL := build--in-container

else  # do a normal build by default without Docker
.DEFAULT_GOAL := build

# On Mac OS X, require GNU coreutils instead of continuing on BSD utilities with uncertainty
ifeq ($(shell uname),Darwin)
ifeq ($(shell brew ls coreutils &>/dev/null || echo notfound),)
    PATH := $(shell brew --prefix coreutils)/libexec/gnubin:$(PATH)
else
    $(error Missing GNU coreutils from Homebrew [http://brew.sh]. \
        It must be installed to ensure correct build. \
        Please run: `brew install coreutils` \
    )
endif
endif

endif  # Docker

### build/test inside containers ##############################################
build--in-container:
	./DockerBuild/build-in-container
test--in-container:
	./DockerBuild/test-in-container-postgres \
	    make -j test $(if $(ONLY),ONLY="$(ONLY)") $(if $(EXCEPT),EXCEPT="$(EXCEPT)")

### dependency recipes ########################################################

.PHONY: depends
depends: .build/depends
.build/depends: util/install.sh $(wildcard util/install/*)
	# Installing and Checking dependencies...
	sha1sum $^ | diff -q $@ - || \
	$< _deepdive_build_deps _deepdive_runtime_deps
	sha1sum $^ >$@

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

# XXX put coffee and node from mindbender on PATH
release-%: PATH := $(realpath $(STAGE_DIR)/mindbender/node_modules/.bin):$(realpath $(STAGE_DIR)/mindbender/depends/bundled/.all/bin):$(PATH)
release-%: GITHUB_REPO = HazyResearch/deepdive
release-%: RELEASE_VERSION = $*
release-%: RELEASE_PACKAGE = deepdive-$(RELEASE_VERSION)-$(shell uname).tar.gz
release-%:
	-git tag --annotate $(RELEASE_VERSION) --cleanup=whitespace
	$(MAKE) RELEASE_VERSION=$(RELEASE_VERSION) $(PACKAGE)
	ln -sfn $(PACKAGE) $(RELEASE_PACKAGE)
	# Releasing $(RELEASE_PACKAGE) to GitHub
	# (Make sure GITHUB_OAUTH_TOKEN is set directly or via ~/.netrc or OS X Keychain)
	util/build/upload-github-release-asset \
	    file=$(RELEASE_PACKAGE) \
	    repo=$(GITHUB_REPO) \
	    tag=$(RELEASE_VERSION)
	# Released $(RELEASE_PACKAGE) to GitHub

# Encrypted deepdiveDeployBot credentials for Travis
.travis.tar.enc: .travis.tar
	travis encrypt-file $< $@
	git add .travis.tar.enc
.travis.tar: .travis
	chmod -R go= .travis
	tar cvf $@ $^

### build recipes #############################################################

# common build steps
BUILD_INFO=$(STAGE_DIR)/.build-info.sh
build:
	# staging all executable code and runtime data under $(STAGE_DIR)/
	./stage.sh $(STAGE_DIR)
	# record version and build info
	util/build/generate-build-info.sh >$(BUILD_INFO)

# how to build external runtime dependencies to bundle
.PHONY: bundled-runtime-dependencies
bundled-runtime-dependencies extern/.build/bundled: extern/bundle-runtime-dependencies.sh
	PACKAGENAME=deepdive  $<
$(PACKAGE): bundled-runtime-dependencies
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

.PHONY: build-mindbender
build-mindbender:
	@util/build/build-submodule-if-needed util/mindbender @prefix@/
build: build-mindbender

.PHONY: build-ddlog
build-ddlog:
	@util/build/build-submodule-if-needed compiler/ddlog target/scala-2.11/ddlog-assembly-0.1-SNAPSHOT.jar
build: build-ddlog

.PHONY: build-mkmimo
build-mkmimo:
	@util/build/build-submodule-if-needed runner/mkmimo mkmimo
build: build-mkmimo
