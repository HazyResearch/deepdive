#!/usr/bin/env bats
# Tests for `deepdive sql` command

. "$BATS_TEST_DIRNAME"/env.sh >&2
PATH="$DEEPDIVE_SOURCE_ROOT/util/test:$PATH"

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
    db-execute "DROP TABLE IF EXISTS foo_load; CREATE TABLE foo_load(a INT, b text, c text[]);"
}

@test "$DBVARIANT deepdive load csv works" {
    deepdive load foo_load "$BATS_TEST_DIRNAME"/test_load/test.csv csv
    [[ $(deepdive sql eval "select * from foo_load" format=csv) = '123,foo bar,"{how,are,you}"' ]]
    db-execute "DROP TABLE IF EXISTS foo_load;"
}

@test "$DBVARIANT deepdive load tsv works" {
    deepdive load foo_load "$BATS_TEST_DIRNAME"/test_load/test.tsv tsv
    [[ $(deepdive sql eval "select * from foo_load" format=csv) = '123,foo bar,"{how,are,you}"' ]]
    db-execute "DROP TABLE IF EXISTS foo_load;"
}
