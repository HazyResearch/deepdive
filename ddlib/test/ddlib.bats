#!/usr/bin/env bats
load test_environ

setup() {
    dd-python2() { deepdive env python2 "$@"; }
    dd-python3() { deepdive env python3 "$@"; }
    cd "$BATS_TEST_DIRNAME"
}

@test "ddlib unit tests in python2" {
    type python2 || skip "no python2"
    dd-python2 test.py
    diff -u test_dep.test2.expected <(bzcat test2.json.bz2 | dd-python2 test_dep.py)
}

@test "ddlib unit tests in python3" {
    type python3 || skip "no python3"
    dd-python3 test.py
    diff -u test_dep.test2.expected <(bzcat test2.json.bz2 | dd-python3 test_dep.py)
}

@test "ddlib gives identical output with and without ddlib in python2" {
    type python2 || skip "no python2"
    diff -u <(bzcat test.json.bz2 | dd-python2 without_ddlib.py) <(bzcat test.json.bz2 | dd-python2 with_ddlib.py)
}

@test "ddlib gives identical output with and without ddlib in python3" {
    type python3 || skip "no python3"
    diff -u <(bzcat test.json.bz2 | dd-python3 without_ddlib.py) <(bzcat test.json.bz2 | dd-python3 with_ddlib.py)
}

@test "ddlib gives identical output with ddlib in python2 and python3" {
    type python2 || skip "no python2"
    type python3 || skip "no python3"
    diff -u <(bzcat test.json.bz2 | dd-python2 with_ddlib.py) <(bzcat test.json.bz2 | dd-python3 with_ddlib.py)
}
