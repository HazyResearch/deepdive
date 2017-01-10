#!/usr/bin/env bash
# A bash script for setting up environment for DeepDive tests that is supposed to be sourced from every .bats

: ${TEST_DEBUG:=}

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

# configure the same environment for tests used by facade command, deepdive
source <("$DEEPDIVE_HOME"/bin/deepdive env bash -c export |
    sed -e '/^declare -/{
        # gets rid of weird declarations without value part
        /=/!d

        # always affect the global scope
        s/^declare -x //
        # XXX if declare is kept without the -g, the effect cannot escape
        #     the function that is calling source and can cause various
        #     confusions.

        # make sure the variable is exported
        p; s/=.*//; s/^/export /
    }')

# turn off progress reporting during tests
DEEPDIVE_SHOW_PROGRESS=false

export \
    DEEPDIVE_TEST_ROOT \
    DEEPDIVE_SOURCE_ROOT \
    DEEPDIVE_SHOW_PROGRESS \
    TEST_DEBUG \
    #

# some BATS utilities for testing

keeping_output_of() {
    if [[ -n $TEST_DEBUG ]]; then
        # keep output of given command when debugging
        "$@" | tee "$BATS_TEST_FILENAME".$BATS_TEST_NUMBER.actual
    else
        # otherwise, just run
        "$@"
    fi
}
