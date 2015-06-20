# Makefile for DeepDive

.DEFAULT_GOAL := install

.PHONY: depends
depends:
	@echo "=== Installing and Checking dependencies... ==="
	util/install.sh deepdive_build_deps deepdive_runtime_deps
	lib/check-depends.sh

.PHONY: build
build:
	@echo "=== Extracting sampler library... ==="
	lib/dw_extract.sh

test install: PATH := $(PATH):$(shell pwd)/sbt

.PHONY: test
test: build
	@echo "\n=== Testing DeepDive modules... ==="
	./test.sh

checkstyle:
	@./test/checkstyle.sh

.PHONY: install
PREFIX = ~/local
DEST = $(PREFIX)/bin
install: depends build
	@echo "\n=== Compiling DeepDive... ==="
	sbt/sbt compile
	@echo "\n=== Installing DeepDive... ==="
	mkdir -p $(DEST)
	ln -sfnv $(realpath shell/deepdive) $(DEST)/
	@if [ -x $(DEST)/deepdive ]; then \
		echo '# DeepDive binary has been install to $(DEST)/.'; \
		echo '# Make sure your shell is configured to include the directory in PATH environment, e.g.:'; \
		echo '  PATH=$(DEST):$$PATH'; \
	fi


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

