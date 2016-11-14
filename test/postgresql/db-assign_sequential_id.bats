#!/usr/bin/env bats

load test_environ

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
}

@test "$DBVARIANT db-assign_sequential_id usage" {
    # make sure it fails on inssuficient arguments
    ! deepdive db assign_sequential_id              || false
    ! deepdive db assign_sequential_id table        || false
}

@test "$DBVARIANT db-assign_sequential_id works" {
    num_failures=0
    for numrows in 0 1 37 1000; do
        # set up a table
        echo "Setting up a table of $numrows rows"
        deepdive sql "DROP TABLE foo CASCADE;" || true
        deepdive sql "CREATE TABLE foo(x TEXT, y BIGINT);"
        seq $numrows | sed 's/.*/INSERT INTO foo(x) VALUES (&);/' | deepdive sql

        begin=0
        end=$(($numrows - 1))
        echo "Testing assign_sequential_id without partition column ($numrows rows)"
        # assign sequence
        deepdive db assign_sequential_id foo y || let ++num_failures
        # and compare with what's expected
        diff -u <(seq $begin $end) <(deepdive sql eval "SELECT y FROM foo ORDER BY y") || let ++num_failures

        echo "Testing assign_sequential_id with partition column ($numrows rows)"
        deepdive db assign_sequential_id foo y "x::int % 6" || let ++num_failures
        # verify num of IDs in partition 0
        diff -u <(echo $(($numrows / 6))) <(deepdive sql eval "SELECT COUNT(1) FROM foo WHERE y <= $numrows") || let ++num_failures

        # verify num of partitions
        if [ $numrows -gt 6 ]; then
            diff -u <(echo 6) <(deepdive sql eval "SELECT COUNT(DISTINCT y >> 48) FROM foo") || let ++num_failures
        fi
    done
    [[ $num_failures -eq 0 ]] || {
        echo "Failed $num_failures checks"
        false
    }
}
