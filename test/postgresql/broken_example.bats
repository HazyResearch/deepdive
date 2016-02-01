#!/usr/bin/env bats
# Tests for broken example

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT DeepDive returns error for a broken application" {
    cd "$BATS_TEST_DIRNAME"/broken_example || skip
    ! deepdive compile ||
    ! deepdive redo process/init/app ||
    ! deepdive redo data/model/{weights,probabilities} || false
}
