#!/usr/bin/env bats
# Tests for multinomial example

. "$BATS_TEST_DIRNAME"/env.sh >&2

setup() {
    cd "$BATS_TEST_DIRNAME"/multinomial_example
}

@test "$DBVARIANT multinomial example gives correct probabilities" {
    ./run.sh
    # Tests are inside run.sh
}

