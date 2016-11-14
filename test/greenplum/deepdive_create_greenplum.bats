#!/usr/bin/env bats
# Tests for `deepdive create` command on Greenplum

load test_environ

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
}

@test "$DBVARIANT deepdive create table foo like bar preserves DISTRIBUTED BY" {
    deepdive sql "DROP VIEW  foo CASCADE" || true
    deepdive sql "DROP TABLE foo CASCADE" || true
    deepdive sql "CREATE TABLE foo(x TEXT, y INT, z INT) DISTRIBUTED BY (y, z)"
    deepdive create table bar like foo
    [ "$(deepdive sql '\d bar' | grep '^Distributed by')" = "Distributed by: (y, z)" ]
}
