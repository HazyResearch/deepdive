#!/usr/bin/env bats
# Tests for `deepdive sql` command

load test_environ
PATH="$DEEPDIVE_SOURCE_ROOT/util/test:$PATH"

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
    db-execute "DROP TABLE IF EXISTS foo_load; CREATE TABLE foo_load(a INT, b text, c text[]);"
    cd "${BATS_TEST_FILENAME%.bats}"
}
teardown() {
    db-execute "DROP TABLE IF EXISTS foo_load;"
}

@test "$DBVARIANT deepdive load csv works" {
    deepdive load foo_load test.csv
    [[ $(deepdive sql eval "select * from foo_load" format=csv) = '123,foo bar,"{how,are,you}"' ]]
}

@test "$DBVARIANT deepdive load tsv works" {
    deepdive load foo_load test.csv
    [[ $(deepdive sql eval "select * from foo_load" format=csv) = '123,foo bar,"{how,are,you}"' ]]
}

@test "$DBVARIANT deepdive load sql works with huge input" {
    n=100 m=99900
    db-execute "DROP TABLE IF EXISTS foo_load; CREATE TABLE foo_load(a INT, b text);"
    {
        seq $m | sed 's/.*/-- comment/'  # generate a huge amount of comments to bloat the input sql file
        seq $n | sed 's/.*/INSERT INTO foo_load(a,b) VALUES (&,'\''&&&&&&&&&'\'');/'
    } >huge.sql
    deepdive load foo_load huge.sql
    [[ $(deepdive sql eval "select count(*) from foo_load") -eq $n ]]
    rm -f huge.sql
}

@test "$DBVARIANT deepdive load with columns works" {
    n=10
    DEEPDIVE_LOAD_FORMAT=tsv  deepdive load "foo_load(a,b)" <(paste <(seq $n) <(yes | head -$n))
    [[ $(deepdive sql eval "select count(*) from foo_load") -eq $n ]]
    [[ $(deepdive sql eval "select sum(a) from foo_load") -eq $(( $n*($n+1)/2 )) ]]
}

@test "$DBVARIANT deepdive load variable table without columns works" {
    n=10
    cd test.app
    deepdive compile
    deepdive sql "DROP TABLE IF EXISTS variable_table_declared_in_app;"
    deepdive db init
    deepdive create table variable_table_declared_in_app
    # deepdive-load should recognize variable tables and load just the three columns defined in DDlog
    DEEPDIVE_LOAD_FORMAT=tsv  deepdive load "variable_table_declared_in_app" <(paste <(yes document_1) <(seq $n) <(seq $n) <(echo 'TRUE'; echo 'FALSE'; yes '\N') <(yes '\N') | head -$n)
    [[ $(deepdive sql eval "select count(*) from variable_table_declared_in_app") -eq $n ]]
    deepdive sql "DROP TABLE IF EXISTS variable_table_declared_in_app;"
    cd -
}
