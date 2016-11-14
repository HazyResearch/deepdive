#!/usr/bin/env bats
# Tests for `deepdive unload` command

load test_environ

N=10000

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
    db-execute "DROP TABLE foo CASCADE;" &>/dev/null || true
    db-execute "CREATE TABLE foo(x INT);"
    db-load "foo" "x" tsv <(seq $N | shuf)
}
teardown() {
    db-execute "DROP TABLE foo CASCADE;"
}

@test "$DBVARIANT deepdive db unload works with single sink" {
    deepdive db unload "SELECT x FROM foo" tsv foo.tsv
    [[ $(wc -l <foo.tsv) -eq $N ]]
    cmp <(sort -n foo.tsv) <(seq $N)
    rm -f foo.tsv
}

@test "$DBVARIANT deepdive db unload works with multiple sinks" {
    rm -f foo-*.tsv
    for nsinks in 2 3 5 8 13; do
        echo Testing $nsinks sinks
        deepdive db unload "SELECT x FROM foo" tsv $(printf "foo-%d.tsv " $(seq $nsinks))
        [[ $(cat foo-*.tsv | wc -l) -eq $N ]]
        cmp <(sort -n foo-*.tsv) <(seq $N)
        rm -f foo-*.tsv
    done
}
