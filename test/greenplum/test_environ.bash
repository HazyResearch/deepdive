#!/usr/bin/env bash
# A script that sets up environment for testing DeepDive against Greenplum

# load common test environment settings
load ../test_environ

export DEEPDIVE_DB_URL="greenplum://${TEST_GREENPLUM_DBHOST:-${TEST_DBHOST:-localhost}}/${TEST_DBNAME:-deepdive_test_$USER}"
. load-db-driver.sh 2>/dev/null

# environment variables expected by Scala test code
export PGDATABASE=$DBNAME  # for testing to work with null settings
export DBCONNSTRING="jdbc:postgresql://$PGHOST:$PGPORT/$DBNAME"
export DEEPDIVE_TEST_ENV="psql"
# for compatibility with psql/mysql generic tests. Should get rid of "PG" stuff.
export DBHOST=$PGHOST
export DBPORT=$PGPORT
export DBPASSWORD=$PGPASSWORD
export DBUSER=$PGUSER

# incremental active
export DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES="r1"
export DEEPDIVE_ACTIVE_INCREMENTAL_RULES="testFactor"
export BASEDIR=$DEEPDIVE_HOME/out


# Greenplum-specific setup
export GPPATH=${GPPATH:-$DEEPDIVE_HOME/out/tmp}
export GPHOST=${GPHOST:-localhost}
export GPPORT=${GPPORT:-$((15433 + ${RANDOM:0:4}))}
export GPLOAD=true

# FIXME launching gpfdist hangs Bats
## Launch gpfdist if not launched.
#gpfdist -d $GPPATH -p $GPPORT &
#gpfdist_pid=$!
#trap "kill $gpfdist_pid" EXIT
