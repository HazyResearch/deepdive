#!/usr/bin/env bats
# Tests for biased coin example

. "$BATS_TEST_DIRNAME"/env.sh >&2

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
}

@test "$DBVARIANT db-assign_sequential_id usage" {
    # make sure it fails on inssuficient arguments
    ! deepdive db assign_sequential_id              || false
    ! deepdive db assign_sequential_id table        || false
    ! deepdive db assign_sequential_id table column || false
}

@test "$DBVARIANT db-assign_sequential_id works" {
    for numrows in 0 1 1000; do
        # set up a table
        echo "Setting up a table of $numrows rows"
        deepdive sql "DROP TABLE foo CASCADE;" || true
        deepdive sql "CREATE TABLE foo(x TEXT, y BIGINT);"
        seq $numrows | sed 's/.*/INSERT INTO foo(x) VALUES (&);/' | deepdive sql

        for increment in 123 10 1; do
            for begin in 12345 -678 1 0; do
                end=$(($begin + ($numrows - 1) * $increment))
                echo "Testing assign_sequential_id from $begin to $end by increment $increment ($numrows rows)"
                # assign sequence
                deepdive db assign_sequential_id foo y $begin $increment
                # and compare with what's expected
                diff -u <(seq $begin $increment $end) <(deepdive sql eval "SELECT y FROM foo ORDER BY y")
            done
        done
    done
}
