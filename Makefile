# Makefile for DeepDive

# install destination
PREFIX = ~/local
# path to the staging area
STAGE_DIR = dist/stage
# path to the area for keeping track of build
BUILD_DIR = .build
# path to the package to be built
PACKAGE = $(dir $(STAGE_DIR))deepdive.tar.gz

NO_DOCKER_BUILD ?= false
ifneq ($(shell $(NO_DOCKER_BUILD) || which docker 2>/dev/null),)
# do a containerized build by default if Docker is available
.DEFAULT_GOAL := build--in-container

else  # do a normal build by default without Docker
.DEFAULT_GOAL := build

endif  # Docker

# On Mac OS X, require GNU coreutils instead of continuing on BSD utilities with uncertainty
# TODO relax this requirement when build--in-container is the only goal
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

### build/test inside containers ##############################################
.PHONY: %--in-container dev-docker-image
dev-docker-image: ./DockerBuild/rebuild-latest-image-from-scratch # needs to be run at least once by someone
	$<
build--in-container: ./DockerBuild/build-in-container
	$<
test--in-container: ./DockerBuild/test-in-container-postgres
	$< \
	    make -j test $(if $(ONLY),ONLY="$(ONLY)") $(if $(EXCEPT),EXCEPT="$(EXCEPT)")
./DockerBuild/%:
	git submodule update --init DockerBuild

### dependency recipes ########################################################

.PHONY: depends
depends: .build/depends
.build/depends: util/install.sh $(wildcard util/install/*)
	# Installing and Checking dependencies...
	mkdir -p $(@D)
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
.PHONY: release-%
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

### production Docker container ##############################################

DOCKER_IMAGE_FOR_BUILD   = hazyresearch/deepdive-build
DOCKER_IMAGE_FOR_RELEASE = hazyresearch/deepdive

.PHONY: production-docker-image
.PHONY: sandbox/deepdive-build.tar.gz sandbox/deepdive-examples.tar.gz  # always retrieve from latest image
production-docker-image: sandbox/Dockerfile sandbox/deepdive-build.tar.gz sandbox/deepdive-examples.tar.gz sandbox/install.sh sandbox/stanford-corenlp
	docker build -t $(DOCKER_IMAGE_FOR_RELEASE) $(<D)
sandbox/deepdive-build.tar.gz:
	docker run --rm -it -v "$(realpath $(@D))":/mnt \
	    $(DOCKER_IMAGE_FOR_BUILD) \
	    tar cfvz /mnt/$(@F) -C "$(STAGE_DIR)" .
sandbox/deepdive-examples.tar.gz:
	docker run --rm -it -v "$(realpath $(@D))":/mnt \
	    $(DOCKER_IMAGE_FOR_BUILD) \
	    tar cfvz /mnt/$(@F) -C examples .
sandbox/install.sh: util/install.sh $(wildcard util/install/*)
	rsync -avH util/install{.sh,} $(@D)/
sandbox/stanford-corenlp:
	mkdir -p $@
	# TODO download latest stable release

### build recipes #############################################################

# common build steps
BUILD_INFO=$(STAGE_DIR)/.build-info.sh
build:
	# staging all executable code and runtime data under $(STAGE_DIR)/
	./stage.sh $(STAGE_DIR)
	# record version and build info
	util/build/generate-build-info.sh >$(BUILD_INFO)

# ensure some submodules required by stage.sh is there
build: \
    extern/buildkit/install-shared-libraries-required-by \
    extern/buildkit/generate-wrapper-for-libdirs \
    #
extern/buildkit/%:
	git submodule update --init extern/buildkit

# how to build external runtime dependencies to bundle
.PHONY: extern bundled-runtime-dependencies all-bundled-runtime-dependencies no-bundled-runtime-dependencies
extern \
bundled-runtime-dependencies \
extern/.build/bundled: extern/bundle-runtime-dependencies.sh extern/bundle.conf
	PACKAGENAME=deepdive  $<
$(PACKAGE): bundled-runtime-dependencies
build: extern/.build/bundled
# bundle all unless overridden with `make no-bundled-runtime-dependencies`
extern/bundle-runtime-dependencies.sh: extern/bundle.conf
all-bundled-runtime-dependencies \
extern/bundle.conf:
	ln -sfn bundle-all.conf extern/bundle.conf
no-bundled-runtime-dependencies:
	ln -sfn bundle-none.conf extern/bundle.conf

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

# submodules to build and the files to copy out from each of them
include util/build/build-submodules.mk
$(BUILD_SUBMODULE)/inference/dimmwitted.mk : COPY = dw
$(BUILD_SUBMODULE)/compiler/ddlog.mk       : COPY = target/scala-2.11/ddlog-assembly-0.1-SNAPSHOT.jar
$(BUILD_SUBMODULE)/runner/mkmimo.mk        : COPY = mkmimo
ifndef NO_MINDBENDER
$(BUILD_SUBMODULE)/util/mindbender.mk      : COPY = @prefix@/
endif

# XXX legacy targets kept to reduce surprise
.PHONY: build-sampler
build-sampler: build-submodule-dimmwitted
.PHONY: build-dimmwitted
build-dimmwitted: build-submodule-dimmwitted
.PHONY: build-mindbender
build-mindbender: build-submodule-mindbender
.PHONY: build-ddlog
build-ddlog: build-submodule-ddlog
.PHONY: build-mkmimo
build-mkmimo: build-submodule-mkmimo
