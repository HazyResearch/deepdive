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

@test "ps_descendants basic" {
    export pidsfile
    : >$pidsfile
    sh -c '
        sleep 0.2 &
        echo $! >>$pidsfile
        wait
    ' &
    pid=$!

    sleep 0.1
    diff -u $pidsfile <(ps_descendants $pid)
}
