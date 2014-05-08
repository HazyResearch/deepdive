# Makefile for DeepDive

.PHONY: build
build:
	sbt compile
	lib/dw_extract.sh

.PHONY: test
test: build
	./test.sh
