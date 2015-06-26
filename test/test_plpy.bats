#!/usr/bin/env bats
# Tests for plpy related code

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "util/ddext.py should work as expected" {
    bash "$DEEPDIVE_HOME"/src/test/python/test_ddext/test.sh
}
