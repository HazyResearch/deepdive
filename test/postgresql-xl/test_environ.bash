#!/usr/bin/env bash
# A script that sets up environment for testing DeepDive against Greenplum

# load common test environment settings
load ../test_environ

export DEEPDIVE_DB_URL="postgresql-xl://${TEST_GREENPLUM_DBHOST:-${TEST_DBHOST:-localhost}}/${TEST_DBNAME:-deepdive_test_$USER}"
. load-db-driver.sh 2>/dev/null

