#!/usr/bin/env bats
# Tests for biased coin example

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT db-assign_sequential_id works" {
    numrows=1000

    # set up a table
    deepdive sql "DROP TABLE foo CASCADE;" || true
    deepdive sql "CREATE TABLE foo(x TEXT, y BIGINT);"
    seq $numrows | sed 's/.*/INSERT INTO foo(x) VALUES (&);/' | deepdive sql

    for increment in 1234 37 23 10 1; do
        for begin in 12345 -678 1 0; do
            end=$(($begin + ($numrows - 1) * $increment))
            echo "Testing assign_sequential_id from $begin to $end by increment $increment ($numrows rows)"
            deepdive db assign_sequential_id foo y $begin $increment
            diff -u <(seq $begin $increment $end) <(deepdive sql eval "SELECT y FROM foo ORDER BY y")
        done
    done
}
