#!/usr/bin/env bats
load test_environ

@test "escape4sh quotes spaces properly" {
    [ "$(sh -xc "$(escape4sh echo a "b c" "d  e" f)")" = "a b c d  e f" ]
}

@test "escape4sh quotes apostrophise and quotes properly" {
    [ "$(sh -xc "$(escape4sh echo "\"I'm your father,\" said  Darth Vader.")")" = '"I'\''m your father," said  Darth Vader.' ]
}

@test "escape4sh works with newlines and special whitespace characters" {
    [ "$(sh -xc "$(escape4sh echo a $'b\n c' $'\td\r\ne' f)")" = $'a b\n c \td\r\ne f' ]
}

@test "escape4sh works on standard input lines with no arguments" {
    [ "$(sh -xc "$(escape4sh < <(printf '%s\n' echo a "b c" "d  e" f))")" = "a b c d  e f" ]
    [ "$(sh -xc "$(escape4sh < <(printf '%s\n' echo "\"I'm your father,\" said  Darth Vader."))")" = '"I'\''m your father," said  Darth Vader.' ]
    [ "$(sh -xc "$(escape4sh < <(printf '%s\n' echo a $'b\b c' $'\td\v\fe' $'f\x1e' $'\x1fg\007'))")" = $'a b\b c \td\v\fe f\x1e \x1fg\007' ]
}

@test "escape4sh gives empty output for no arguments nor input" {
    [ -z "$(escape4sh </dev/null)" ]
}

@test "escape4sh handles many arguments correctly" {
    nargs=100
    eval "set -- $(seq $nargs | sed 's/^/"arg  /; s/$/"/' | tr '\n' ' ')"
    diff -u <(printf "found\targ  %s\n" $(seq $nargs)) <(sh -xc "$(escape4sh printf 'found\t%s\n' "$@")")
}
