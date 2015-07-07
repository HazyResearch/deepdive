#!/usr/bin/env bats
# Tests for chunking example

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT chunking example" {
    cd "$BATS_TEST_DIRNAME"/chunking_example || skip
    deepdive initdb
    deepdive run

    f1score=$(printf '%.0f' $(result/eval.sh | sed -n '/^accuracy:/ s/.* FB1: *//p'))
    [[ $f1score -ge 80 ]]
}
