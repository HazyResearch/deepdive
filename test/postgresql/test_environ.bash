#!/usr/bin/env bash
# A script that sets up environment for testing DeepDive against PostgreSQL

# load common test environment settings
load ../test_environ

# initialize database
: ${DEEPDIVE_DB_URL:=postgresql://${TEST_POSTGRES_DBHOST:-${TEST_DBHOST:-localhost}}/${TEST_DBNAME:-deepdive_test_$USER}}
. load-db-driver.sh 2>/dev/null

# environment variables expected by Scala test code
export PGDATABASE=$DBNAME  # for testing to work with null settings
export DBCONNSTRING=jdbc:postgresql://$PGHOST:$PGPORT/$DBNAME
export DEEPDIVE_TEST_ENV="psql"
# for compatibility with psql/mysql generic tests. Should get rid of "PG" stuff.
export DBHOST=$PGHOST
export DBPORT=$PGPORT
export DBPASSWORD=$PGPASSWORD
export DBUSER=$PGUSER

# incremental active
export DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES="r1"
export DEEPDIVE_ACTIVE_INCREMENTAL_RULES="testFactor"
export BASEDIR="$PWD"/out  # XXX out/ under the working directory happens to be DeepDive's default output path
