# A Makefile fragment for running tests with BATS
#
# Author: Jaeho Shin <netj@cs.stanford.edu>
# Created: 2015-07-09

# some default locations
TEST_ROOT = test
TEST_LIST_COMMAND = $(TEST_ROOT)/enumerate-tests.sh
BATS_ROOT = $(TEST_ROOT)/bats
export TEST_ROOT

.PHONY: test test-build test-list
# One can fine-tune the list of tests by setting environment variables
# TEST_ONLY and TEST_EXCEPT, or passing the same glob patterns as Make
# arguments ONLY and EXCEPT, e.g.:
#   $ make test ONLY+=$(TEST_ROOT)/foo/*.bats EXCEPT+=$(TEST_ROOT)/*/unit_tests.bats
#   $ TEST_ONLY=$(TEST_ROOT)/foo/*.bats  make test EXCEPT+=$(TEST_ROOT)/*/unit_tests.bats
#   $ TEST_ONLY=$(TEST_ROOT)/foo/*.bats TEST_EXCEPT=$(TEST_ROOT)/*/unit_tests.bats   make test
TEST_ONLY ?= $(shell $(TEST_LIST_COMMAND))
TEST_EXCEPT ?=
test: ONLY = $(TEST_ONLY)
test: EXCEPT = $(TEST_EXCEPT)
test: BATS_FILES = $(filter-out $(wildcard $(EXCEPT)),$(wildcard $(ONLY)))
test: $(BATS_ROOT)/bin/bats $(BATS_FILES) test-build
	# Running $(shell $(TEST_ROOT)/bats/bin/bats -c $(BATS_FILES)) tests defined in $(words $(BATS_FILES)) .bats files
	#  To test selectively, run:  make test   ONLY+=/path/to/bats/files
	#  To exclude certain tests:  make test EXCEPT+=/path/to/bats/files
	#  For a list of tests, run:  make test-list
	$(BATS_ROOT)/bin/bats  $(BATS_FILES)
test-build:
$(BATS_ROOT)/bin/bats:
	git submodule update --init $(BATS_ROOT)
test-list:
	@echo "make test \\"
	@$(TEST_LIST_COMMAND) | sed 's/$$/ \\/; s/^/   ONLY+=/p; s/^   ONLY+=/ EXCEPT+=/'
	@echo " #"

