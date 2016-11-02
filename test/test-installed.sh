#!/usr/bin/env bash
# Run tests against the installed version of DeepDive
set -eu
cd "$(dirname "$0")"/.. # move to top of DeepDive's source tree

# find which DeepDive installation we're testing against from $PATH
DEEPDIVE_HOME=$(deepdive env sh -c 'echo "$DEEPDIVE_HOME"') || {
    echo >&2 "No deepdive command found on PATH"
    false
}
export DEEPDIVE_HOME
echo "Testing DeepDive installed at: $DEEPDIVE_HOME"

# make sure we have BATS
[[ -x test/bats/bin/bats ]] ||
    git submodule update --init test/bats ||
    git clone https://github.com/sstephenson/bats.git test/bats ||
    curl -fsSL https://github.com/sstephenson/bats/archive/v0.4.0.tar.gz |
        (cd test && rm -rf bats && tar xfz - && mv -f bats-0.4.0 bats)

# display target database
# TODO read these default values from somewhere else
echo "Testing against database TEST_DBNAME=${TEST_DBNAME:-deepdive_test_$USER} running at TEST_DBHOST=${TEST_DBHOST:-localhost}"

# make sure numeric handling are done in the expected way
export LC_NUMERIC=C

# run tests
[[ $# -gt 0 ]] || set -- $(test/enumerate-tests.sh | grep -v scalatests)
echo "Running $(test/bats/bin/bats -c "$@") tests defined in $# .bats files: $*"
[[ $# -gt 0 ]] || echo " (To test selectively, pass the .bats files over command-line)"
test/bats/bin/bats "$@"
