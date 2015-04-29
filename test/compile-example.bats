#!/usr/bin/env bats

# required variables
: ${DDLOG_JAR:?path to ddlog jar}
: ${EXAMPLE:?path to input ddlog program}
EXAMPLE_NAME=${EXAMPLE%.ddl}
EXAMPLE_NAME=${EXAMPLE_NAME##*/}

setup() {
    [ -e "$DDLOG_JAR" ]
    [ -e "$EXAMPLE" ]
}

@test "compile $EXAMPLE_NAME" {
    expectedOutput=${EXAMPLE%.ddl}.compile.expected
    [ -e "$expectedOutput" ] || skip
    scala "$DDLOG_JAR" compile "$EXAMPLE" |
    diff -u "$expectedOutput" -
}
