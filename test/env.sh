#!/usr/bin/env bash
# A bash script for setting up environment for DeepDive tests that is supposed to be sourced from every .bats

# find out the path to the root of DeepDive's source tree
DEEPDIVE_TEST_ROOT=$(cd "$(dirname "${BASH_SOURCE:-$0}")" && pwd)
DEEPDIVE_SOURCE_ROOT=$(cd "$DEEPDIVE_TEST_ROOT/.." && pwd)
DEEPDIVE_HOME=$DEEPDIVE_SOURCE_ROOT

# configure PATH and CLASSPATH for tests
PATH="$DEEPDIVE_SOURCE_ROOT/shell:$DEEPDIVE_SOURCE_ROOT/util:$DEEPDIVE_SOURCE_ROOT/sbt:$PATH"
CLASSPATH="$(cat "$DEEPDIVE_TEST_ROOT"/.classpath)${CLASSPATH:+:$CLASSPATH}"

export \
    DEEPDIVE_TEST_ROOT \
    DEEPDIVE_SOURCE_ROOT \
    DEEPDIVE_HOME \
    PATH \
    CLASSPATH \
    #
