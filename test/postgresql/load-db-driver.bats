#!/usr/bin/env bats
# Tests for load-db-driver.sh
load test_environ

@test "db.url allows use of environment variables" {
    dbname=$(
        cd "$BATS_TEST_DIRNAME"/deepdive_load/test.app
        unset DEEPDIVE_DB_URL
        USER=foobar
        . load-db-driver.sh
        echo "$DBNAME"
    )
    [[ "$dbname" = "deepdive_test_foobar" ]]
}
