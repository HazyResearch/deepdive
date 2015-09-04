#!/usr/bin/env bats
# Tests for initdb

. "$BATS_TEST_DIRNAME"/env.sh >&2

setup() {
    cd "$BATS_TEST_DIRNAME"/spouse_example
}

@test "$DBVARIANT initdb from ddlog" {
    cd ddlog || skip
    deepdive initdb articles
    [[ $(deepdive sql eval "SELECT * FROM articles" format=csv header=1) = 'article_id,text' ]]
    deepdive sql "INSERT INTO articles VALUES ('foo', 'bar')"
}
