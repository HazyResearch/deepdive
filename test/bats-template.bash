#!/usr/bin/env bash
# Utilities for testing with Bats

# some shorthands
TESTDIR=${BATS_TEST_FILENAME%.bats}
TESTDIR=${TESTDIR#$PWD/}
TEST=${BATS_TEST_FILENAME#$PWD/}
it="${TEST%.bats}:"

# how to invoke ddlog compiler
ddlog() {
    java org.deepdive.ddlog.DeepDiveLog "$@"
}

# how to diff (prefer wdiff)
if type wdiff &>/dev/null; then
    if ${TRAVIS:-false}; then
        diff() { wdiff            --statistics -- "$@"; }
    else
        diff() { wdiff --terminal --statistics -- "$@"; }
    fi
else
    diff() { command diff --unified --ignore-all-space -- "$@"; }
fi

