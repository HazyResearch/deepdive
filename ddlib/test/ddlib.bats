#!/usr/bin/env bats
. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "ddlib gives identical output with and without ddlib in python2" {
	cat test.json | python without_ddlib.py > a
	cat test.json | python with_ddlib.py > b
	diff a b
	rm a
	rm b
}

@test "ddlib gives identical output with and without ddlib in python3" {
	cat test.json | python3 without_ddlib.py > a
	cat test.json | python3 with_ddlib.py > b
	diff a b
	rm a
	rm b
}

@test "ddlib gives identical output with ddlib in python2 and python3" {
	cat test.json | python with_ddlib.py > a
	cat test.json | python3 with_ddlib.py > b
	diff a b
	rm a
	rm b
}
