#!/usr/bin/env bats
# Tests for util/partition_id_range
load test_environ

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
}

@test "$DBVARIANT partition_id_range works with a single table" {
    N=100
    # create a table for test
    deepdive sql "
        DROP TABLE IF EXISTS foo;
        CREATE TABLE foo(x INT);
        INSERT INTO foo(x) VALUES (0)
        $(seq $(($N - 1)) | awk '{print ", (" $0 ")"}');
    "
    # for a range of different starting point
    for RANGE_BEGIN in 0 123 -456 99999999999999 -999999999999; do
        # partition and check if the output is correct
        echo Trying RANGE_BEGIN=$RANGE_BEGIN
        RANGE_BEGIN=$RANGE_BEGIN \
        partition_id_range foo | diff <(echo foo$'\t'$RANGE_BEGIN$'\t'$(( $RANGE_BEGIN + $N ))) -
    done
    # clean up
    deepdive sql "DROP TABLE foo;"
}

@test "$DBVARIANT partition_id_range works with many tables" {
    T=${RANDOM:0:1} # number of tables <= 10
    let ++T # which needs to be at least one
    # create many tables for test with random cardinalities
    counts=(0) tables=
    for t in $(seq $T); do
        counts+=(${RANDOM:0:3})
        deepdive sql "
            DROP TABLE IF EXISTS foo_$t;
            CREATE TABLE foo_$t(x INT);
            INSERT INTO foo_$t(x) VALUES (0)
            $(seq $((${counts[$t]} - 1)) | awk '{print ", (" $0 ")"}');
        "
        tables+=" foo_$t"
    done
    # for a range of different starting point
    for RANGE_BEGIN in 0 123 -456 99999999999999 -999999999999; do
        # define what we expect
        expected_output=$(
            c=$RANGE_BEGIN
            for t in $(seq $T); do
                echo foo_$t$'\t'$c$'\t'$(( $c + ${counts[$t]} ))
                let c+=${counts[$t]}
            done
        )
        # partition and check if the output is correct
        echo Trying RANGE_BEGIN=$RANGE_BEGIN
        RANGE_BEGIN=$RANGE_BEGIN \
        partition_id_range $tables | diff <(echo "$expected_output") -
    done
    # clean up
    for t in $(seq $T); do
        deepdive sql "DROP TABLE foo_$t;"
    done
}

# TODO test RANGE_STEP

@test "$DBVARIANT partition_id_range fails on empty tables" {
    ! partition_id_range || false
}

# TODO add more tests
