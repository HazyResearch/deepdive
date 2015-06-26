#!/usr/bin/env bash
# A script that sets up environment for testing DeepDive against MySQL

# load common test environment settings
. "${BASH_SOURCE%/*}"/../env.sh

# initialize database
: ${DEEPDIVE_DB_URL:=mysql://${TEST_DBHOST:-root@localhost}/${TEST_DBNAME:-deepdive_test_$USER}}
. load-db-driver.sh

# environment variables expected by Scala test code
export PGDATABASE=$DBNAME  # for testing to work with null settings
export DBCONNSTRING=jdbc:mysql://$DBHOST:$DBPORT/$DBNAME
export DEEPDIVE_TEST_ENV="mysql"

# for compatibility with psql/mysql generic tests. Should get rid of "PG" stuff.
export PGHOST=$DBHOST
export PGPORT=$DBPORT
export PGPASSWORD=$DBPASSWORD
export PGUSER=$DBUSER
