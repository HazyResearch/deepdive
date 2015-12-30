# A Makefile fragment for running tests with BATS
#
# Author: Jaeho Shin <netj@cs.stanford.edu>
# Created: 2015-07-09

# some default locations
TEST_ROOT = test
BATS_ROOT = $(TEST_ROOT)/bats
# a command to enumerate all .bats files to test
TEST_LIST_COMMAND = find $(TEST_ROOT) -path $(BATS_ROOT) -prune -false -o -name '*.bats'
export TEST_ROOT

.PHONY: test test-build test-list
test-list:
	@echo "make test \\"
	@$(TEST_LIST_COMMAND) | sed 's/$$/ \\/; s/^/   ONLY+=/p; s/^   ONLY+=/ EXCEPT+=/'
	@echo " #"

test: test-build
test-build:

test: $(BATS_ROOT)/bin/bats
$(BATS_ROOT)/bin/bats:
	git submodule update --init $(BATS_ROOT)
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
.SECONDEXPANSION:
test: $$(BATS_FILES)
test:
	# Running $(shell $(TEST_ROOT)/bats/bin/bats -c $(BATS_FILES)) tests defined in $(words $(BATS_FILES)) .bats files
	#  To test selectively, run:  make test   ONLY+=/path/to/bats/files
	#  To exclude certain tests:  make test EXCEPT+=/path/to/bats/files
	#  For a list of tests, run:  make test-list
	@if [ $(words $(BATS_FILES)) -gt 0 ]; \
	then \
	    echo "$(BATS_ROOT)/bin/bats \\"; \
	        printf '  %s \\\n' $(BATS_FILES); \
	        echo '  #'; \
	    $(BATS_ROOT)/bin/bats  $(BATS_FILES); \
	fi
