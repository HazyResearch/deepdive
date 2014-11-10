# Makefile for DeepDive

.PHONY: build
build:
	@echo "=== Extracting sampler library... ==="
	lib/dw_extract.sh

	@echo "\n=== Compiling DeepDive... ==="
	sbt/sbt pack

	@echo "\n=== Installing DeepDive... ==="
	make -C target/pack/ install ; 

	@echo "\n=== Verifying installation... ==="
	@if [ -f ${HOME}/local/bin/deepdive ]; then \
		echo "SUCCESS! DeepDive binary has been put into ${HOME}/local/bin."; \
		echo "Make sure you set environment variables for sampler before running deepdive. See: http://deepdive.stanford.edu/doc/basics/installation.html"; \
	else \
		echo "FAILED."; \
		exit 1; \
	fi 
	

.PHONY: test
test: 
	@echo "\n=== Testing DeepDive modules... ==="
	./test.sh

.PHONY: all
all: build test

.DEFAULT_GOAL:= build   # `make` only do installation for now. 
						# testing needs `make test` on users' will

