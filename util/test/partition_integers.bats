#!/usr/bin/env bats
load test_environ

cd "$BATS_TMPDIR"

check_correct_output() {
    local m=$1 n=$2
    # crunch actual output
    cat >actual_output
    wc -l <actual_output >num_lines
    ((sed 's/ .*$//' | tr '\n' +; echo 0) | bc) <actual_output >sum
    (sed 's/ /+(/; s/ /-/; s/$/)/' | bc | grep -nvxF '1'  | sed 's/:.*//') <actual_output >incorrect_lines
    rm -f actual_output
    # check number of partitions
    local n_actual=$(cat num_lines; rm -f num_lines)
    echo "num_output_partitions: $n_actual = $n ?"
    [[ $n_actual -eq $n ]]
    # check if they all sum up to m
    local sum=$(cat sum; rm -f sum)
    echo "sum_partition_sizes: $sum = $m ?"
    [[ $sum -eq $m ]]
    # check if any line contained incorrect ranges
    ! [[ -s incorrect_lines ]] || {
        echo "incorrect ranges at lines:"
        cat incorrect_lines; rm -f incorrect_lines
        false
    }
}

@test "partition_integers usage" {
    ! partition_integers || false
    ! partition_integers 10 || false
    ! partition_integers 10 two || false
    ! partition_integers ten 2 || false
    ! partition_integers ten two || false
    partition_integers 10 2 >/dev/null
}

@test "partition_integers works" {
    for m in 0 10 100 131 437 1723; do
        for n in 2 5 7 11 102 135 207 1724; do
            echo "partitioning $m by $n"
            partition_integers $m $n | check_correct_output $m $n
        done
    done
}

@test "partition_integers fails on non-positive partitions" {
    for m in 10 100; do
        for n in 0 -2 -101; do
            echo "partitioning $m by $n"
            ! partition_integers $m $n >/dev/null || false
        done
    done
    for m in -200 -1000; do
        for n in 0 10 100 -2 -101; do
            echo "partitioning $m by $n"
            ! partition_integers $m $n >/dev/null || false
        done
    done
}
