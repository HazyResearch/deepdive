#!/usr/bin/env bash
# A bash script for setting up environment for DeepDive tests that is supposed to be sourced from every .bats

# find out the path to the root of DeepDive's source tree
DEEPDIVE_TEST_ROOT=$(cd "$(dirname "${BASH_SOURCE:-$0}")" && pwd)
DEEPDIVE_SOURCE_ROOT=$(cd "$DEEPDIVE_TEST_ROOT/.." && pwd)

# determine the DeepDive installation to run tests against (defaults to the staged one)
: ${DEEPDIVE_HOME:=$(cd "$DEEPDIVE_SOURCE_ROOT" && echo "$PWD"/dist/stage)}

# make sure there's actually DeepDive to test against
# TODO more sophisticated detection
[[ -d "$DEEPDIVE_HOME" ]] || {
    echo >&2 "$DEEPDIVE_HOME: No DeepDive installation found."
    case $DEEPDIVE_HOME in
        */dist/stage)
            echo >&2 'Did you forget to run `make test-build` first?'
    esac
    false
}

# configure PATH and CLASSPATH for tests
PATH="$DEEPDIVE_HOME/util:$DEEPDIVE_HOME/bin:$PATH"
! [[ -r "$DEEPDIVE_HOME"/env.sh ]] || source "$DEEPDIVE_HOME"/env.sh

export \
    DEEPDIVE_TEST_ROOT \
    DEEPDIVE_SOURCE_ROOT \
    DEEPDIVE_HOME \
    PATH \
    CLASSPATH \
    #
