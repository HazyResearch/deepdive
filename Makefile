# Makefile for DeepDive

.PHONY: build
build:
	@echo "=== Extracting sampler library... ==="
	lib/dw_extract.sh

	@echo "\n=== Compiling DeepDive... ==="
	sbt/sbt pack

	@echo "\n=== Installing DeepDive... ==="
	$(MAKE) -C target/pack/ install ; 

	@echo "\n=== Verifying installation... ==="
	@if [ -f ${HOME}/local/bin/deepdive ]; then \
		echo "SUCCESS! DeepDive binary has been put into ${HOME}/local/bin."; \
		echo "Make sure you set environment variables for sampler before running deepdive. See: http://deepdive.stanford.edu/doc/basics/installation.html"; \
	else \
		echo "FAILED."; \
		exit 1; \
	fi 
	
.PHONY: build-sampler
build-sampler:
	git submodule update --init
	[ -e sampler/lib/gtest -a -e sampler/lib/tclap ] || $(MAKE) -C sampler dep
	$(MAKE) -C sampler dw
ifeq ($(shell uname),Linux)
	cp -f sampler/dw util/sampler-dw-linux
endif
ifeq ($(shell uname),Darwin)
	cp -f sampler/dw util/sampler-dw-mac
endif

.PHONY: test
test: 
	@echo "\n=== Testing DeepDive modules... ==="
	./test.sh

.PHONY: build-mindbender
build-mindbender:
	$(MAKE) -C mindbender
	cp -f mindbender/mindbender-LATEST-*.sh util/mindbender

.PHONY: all
all: build test

.DEFAULT_GOAL:= build   # `make` only do installation for now. 
						# testing needs `make test` on users' will

