#!/usr/bin/env bash
# Utilities for testing with Bats

# required variables
: ${TEST_CLASS_OR_JAR:?class name or -jar with the path to the ddlog.jar to test}

# some shorthands
TESTDIR=${BATS_TEST_FILENAME%.bats}
TESTDIR=${TESTDIR#$PWD/}
TEST=${BATS_TEST_FILENAME#$PWD/}
it="${TEST%.bats}:"

# how to invoke ddlog compiler
ddlog() {
    java $TEST_CLASS_OR_JAR "$@"
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

