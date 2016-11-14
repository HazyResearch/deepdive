#!/usr/bin/env bats
# Tests for `deepdive create` command

load test_environ

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
}

numcolumns() {
    local table=$1; shift
    deepdive sql eval "
        SELECT COUNT(*)
          FROM information_schema.columns
         WHERE table_name = '$table'
    "
}

@test "$DBVARIANT deepdive create fails on incorrect usage" {
    ! deepdive create || false
    ! deepdive create table || false
    ! deepdive create view || false
    ! deepdive create table-if-not-exists || false
    ! deepdive create view-if-not-exists || false
    ! deepdive create table foo as || false
    ! deepdive create table as foo || false
    ! deepdive create table foo like || false
    ! deepdive create table like bar || false
    ! deepdive create something awesome please || false
    # TODO check if usage is shown
}

@test "$DBVARIANT deepdive create table foo(x INT) and foo(y INT, z TEXT)" {
    deepdive sql "DROP VIEW  foo" || true
    deepdive sql "DROP TABLE foo" || true
    [ $(numcolumns foo) -eq 0 ]

    deepdive create table foo x:INT
    [ $(numcolumns foo) -eq 1 ]

    deepdive create table foo y:INT z:TEXT
    [ $(numcolumns foo) -eq 2 ]
}

@test "$DBVARIANT deepdive create table foo as SELECT query" {
    deepdive create table foo as "SELECT 123 AS x"
    [ $(numcolumns foo) -eq 1 ]
    [ $(deepdive sql eval "SELECT SUM(x) FROM foo") -eq 123 ]

    # only SELECT queries should work
    ! deepdive create table foo as "INSERT INTO foo VALUES (456)" || false
    [ $(numcolumns foo) -eq 0 ]
}

@test "$DBVARIANT deepdive create table foo like bar" {
    deepdive create table foo as "SELECT 123 AS x"
    [ $(numcolumns foo) -eq 1 ]
    [ $(deepdive sql eval "SELECT SUM(x) FROM foo") -eq 123 ]

    deepdive create table bar like foo
    [ $(numcolumns bar) -eq 1 ]
    [ $(deepdive sql eval "SELECT SUM(x) FROM foo") -eq 123 ]
    [ $(deepdive sql eval "SELECT COUNT(*) FROM bar") -eq 0 ]

    deepdive create table foo as "SELECT 456 AS x"
    [ $(numcolumns bar) -eq 1 ]
    [ $(deepdive sql eval "SELECT SUM(x) FROM foo") -eq 456 ]
    [ $(deepdive sql eval "SELECT COUNT(*) FROM bar") -eq 0 ]
}

@test "$DBVARIANT deepdive create table-if-not-exists foo(y INT, z TEXT) after foo(x INT)" {
    deepdive sql "DROP VIEW  foo" || true
    deepdive sql "DROP TABLE foo" || true
    [ $(numcolumns foo) -eq 0 ]

    # the following should create the table
    deepdive create table-if-not-exists foo y:INT z:TEXT
    [ $(numcolumns foo) -eq 2 ]

    # the following should drop and create the table
    deepdive create table foo x:INT
    [ $(numcolumns foo) -eq 1 ]

    # but the following should not replace the table
    deepdive create table-if-not-exists foo y:INT z:TEXT
    [ $(numcolumns foo) -eq 1 ]
}


@test "$DBVARIANT deepdive create view bar as SELECT query" {
    # create the view
    deepdive create view bar as "SELECT 123 AS x"
    [ $(deepdive sql eval "SELECT SUM(x) FROM bar") -eq 123 ]

    # replace the view
    deepdive create view bar as "SELECT 456 AS x"
    [ $(deepdive sql eval "SELECT SUM(x) FROM bar") -eq 456 ]

    # see if it is really a view
    deepdive create table foo x:INT
    deepdive create view bar as "SELECT * FROM foo"
    [ $(numcolumns bar) -eq 1 ]
    [ $(deepdive sql eval "SELECT COUNT(*) FROM bar") -eq 0 ]
    deepdive sql "INSERT INTO foo(x) VALUES (123)"
    [ $(deepdive sql eval "SELECT SUM(x) FROM bar") -eq 123 ]
}

@test "$DBVARIANT deepdive create view bar as with only SELECT query" {
    # only SELECT queries should work
    ! deepdive create view bar as "INSERT INTO foo VALUES (789)" ||
        skip "Did not return error"  # XXX Greenplum does not exit with non-zero status upon syntax error
    [ $(numcolumns bar) -eq 0 ]
    [ $(numcolumns foo) -eq 1 ]
}

@test "$DBVARIANT deepdive create table foo outside app fails" {
    mkdir -p tmp || skip "Cannot create directory: tmp"
    cd tmp
    ! deepdive create table foo || false
    cd -
    rmdir tmp
}

@test "$DBVARIANT deepdive create table inside app" {
    cd "$BATS_TEST_DIRNAME"/deepdive_load/test.app
    deepdive compile
    deepdive db init

    # works for relations defined in DDlog/schema
    deepdive create table variable_table_declared_in_app
    [ $(numcolumns variable_table_declared_in_app) -gt 0 ]

    # fails for others
    ! deepdive create table nonexistent || false
    [ $(numcolumns nonexistent) -eq 0 ]

    # should allow overriding schema relations (with warnings)
    deepdive create table variable_table_declared_in_app x:INT
    [ $(numcolumns variable_table_declared_in_app) -eq 1 ]

    cd -
}
