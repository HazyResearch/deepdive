#!/usr/bin/env bats
# Per example compilation test
#
# This test compares the `ddlog compile` output of a .ddl file with its expected output in .compile.expected file.
# Test is skipped if no expected output is there.

# required variables
: ${DDLOG_JAR:?path to ddlog jar}
: ${EXAMPLE:?path to input ddlog program}
EXAMPLE_NAME=${EXAMPLE%.ddl}
EXAMPLE_NAME=${EXAMPLE_NAME##*/}

# some preconditions
setup() {
    [ -e "$DDLOG_JAR" ]
    [ -e "$EXAMPLE" ]
}

# actual test that compares the compiled output
@test "compile $EXAMPLE_NAME" {
    expectedOutput=${EXAMPLE%.ddl}.compile.expected
    [ -e "$expectedOutput" ] || skip
    scala "$DDLOG_JAR" compile "$EXAMPLE" |
    diff -u "$expectedOutput" -
}
