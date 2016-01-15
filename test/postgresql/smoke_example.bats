#!/usr/bin/env bats
# Tests for smoke example

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT smoke example" {
    cd "${BATS_TEST_FILENAME%.bats}"/ || skip
    deepdive compile
    deepdive redo process/init/app data/model/probabilities
}
