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
    num_failures=0
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
                deepdive db assign_sequential_id foo y $begin $increment || let ++num_failures
                # and compare with what's expected
                diff -u <(seq $begin $increment $end) <(deepdive sql eval "SELECT y FROM foo ORDER BY y") || let ++num_failures
            done
        done

        echo "Testing if zero increment is rejected"
        ! deepdive db assign_sequential_id foo y $begin 0 || let ++num_failures
    done
    [[ $num_failures -eq 0 ]] || {
        echo "Failed $num_failures checks"
        false
    }
}

@test "$DBVARIANT db-assign_sequential_id works with ORDER_BY constraint" {
    numrows=1000
    echo "populating table foo..."
    deepdive sql "DROP TABLE foo CASCADE;" || true
    deepdive sql "CREATE TABLE foo(x TEXT, y BIGINT, z DOUBLE PRECISION);"
    seq $numrows | sed 's/.*/INSERT INTO foo(x) VALUES (&);/' | deepdive sql >/dev/null

    # set a column to random numbers
    echo "randomizing column z..."
    deepdive sql "UPDATE foo SET z = RANDOM()"

    # assign id according to the order of the randomized column
    echo "db-assign_sequential_id..."
    deepdive db assign_sequential_id foo y 1 1 "z"

    # check if the order is correct
    echo "checking order..."
    incorrect_rows=$(deepdive sql eval "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (ORDER BY z) AS rn FROM foo) bar WHERE rn != y" format=tsv)
    echo "$incorrect_rows"
    [[ -z $incorrect_rows ]]
}
