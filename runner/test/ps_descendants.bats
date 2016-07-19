#!/usr/bin/env bats
. "$BATS_TEST_DIRNAME"/env.sh

setup() {
    pidsfile=$(mktemp "$BATS_TMPDIR"/ps_descendants-pids.XXXXXXX)
}
teardown() {
    rm -f $pidsfile
}

@test "ps_descendants usage" {
    # should accept exactly one PID
    ps_descendants 1
    ! ps_descendants || false
    ! ps_descendants 123 456 || false
}

@test "ps_descendants works with descendants" {
    export pidsfile
    : >$pidsfile
    bash -c '
        sleep 5 &
        echo $! >>$pidsfile
        wait
    ' &
    pid=$!
    sleep 0.1

    diff -u <(sort -n $pidsfile) <(ps_descendants $pid | sort -n)
    kill $(cat $pidsfile)
}

@test "ps_descendants works with process group" {
    export pidsfile
    : >$pidsfile
    set -m
    bash -c '
        sleep 5 &
        echo $! >>$pidsfile
        disown
        sleep 5 &
        echo $! >>$pidsfile
        wait
    ' &
    pid=$!
    sleep 0.1

    diff -u <(sort -n $pidsfile) <(ps_descendants $pid | sort -n)
    kill $(cat $pidsfile)
}
