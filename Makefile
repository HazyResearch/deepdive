# Makefile for DeepDive

.PHONY: build
build:
	@echo -e "=== Extracting sampler library... ==="
	lib/dw_extract.sh

	@echo -e "\n=== Compiling DeepDive... ==="
	sbt/sbt pack

	@echo -e "\n=== Installing DeepDive... ==="
	make -C target/pack/ install ; 

	@echo -e "\n=== Verifying installation... ==="
	@if [ -f ${HOME}/local/bin/deepdive ]; then \
		echo "SUCCESS! DeepDive binary has been put into ${HOME}/local/bin."; \
		echo "Make sure you set environment variables for sampler before running deepdive. See: http://deepdive.stanford.edu/doc/sampler.html"; \
	else \
		echo "FAILED."; \
		exit 1; \
	fi 
	

.PHONY: test
test: 
	@echo -e "\n=== Testing DeepDive modules... ==="
	./test.sh

.PHONY: all
all: build test

.DEFAULT_GOAL:= all

