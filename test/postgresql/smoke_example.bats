#!/usr/bin/env bats
# Tests for smoke example

load test_environ

@test "$DBVARIANT smoke example" {
    cd "${BATS_TEST_FILENAME%.bats}"/ || skip
    deepdive compile
    deepdive redo process/init/app data/model/probabilities
}
