#!/usr/bin/env bats
. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "escape4sh quotes spaces properly" {
    [ "$(sh -xc "$(escape4sh echo a "b c" "d  e" f)")" = "a b c d  e f" ]
}

@test "escape4sh quotes apostrophise and quotes properly" {
    [ "$(sh -xc "$(escape4sh echo "\"I'm your father,\" said  Darth Vader.")")" = '"I'\''m your father," said  Darth Vader.' ]
}

@test "escape4sh gives empty output for no arguments" {
    [ -z "$(escape4sh)" ]
}
