#!/usr/bin/env bats
. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "ddlib gives identical output with and without ddlib in python2" {
    diff -u <(bzcat test.json.bz2 | python2 without_ddlib.py) <(bzcat test.json.bz2 | python2 with_ddlib.py)
}

@test "ddlib gives identical output with and without ddlib in python3" {
    diff -u <(bzcat test.json.bz2 | python3 without_ddlib.py) <(bzcat test.json.bz2 | python3 with_ddlib.py)
}

@test "ddlib gives identical output with ddlib in python2 and python3" {
    diff -u <(bzcat test.json.bz2 | python2 with_ddlib.py) <(bzcat test.json.bz2 | python3 with_ddlib.py)
}
