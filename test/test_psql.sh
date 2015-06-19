#! /usr/bin/env bash
set -x

# Set username and password
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}
export DBNAME=deepdive_test
export PGDATABASE=$DBNAME  # for testing to work with null settings
export GPHOST=${GPHOST:-localhost}
export GPPORT=${GPPORT:-8082}
export GPPATH=${GPPATH:-/tmp}
export DBCONNSTRING=jdbc:postgresql://$PGHOST:$PGPORT/$DBNAME
echo "CONN STRING: $DBCONNSTRING"
export DEEPDIVE_HOME=`cd $(dirname $0)/../; pwd`

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

cd $DEEPDIVE_HOME

# Create test database
dropdb $DBNAME
createdb $DBNAME

# Run the test

# Separate different tests to fix the issue of unable to run multiple integration tests. If any of the tests return non-0 value, exit with the error code.
export SBT_OPTS="-XX:MaxHeapSize=256m -Xmx512m -XX:MaxPermSize=256m"
# # Test argument "-- -oF" shows full stack trace when error occurs
sbt coverage "test-only org.deepdive.test.unit.* -- -oF" && \
sbt coverage "test-only org.deepdive.test.integration.BrokenTest -- -oF" && \
sbt coverage "test-only org.deepdive.test.integration.BiasedCoin -- -oF" && \
sbt coverage "test-only org.deepdive.test.integration.PostgresSpouseExample -- -oF" && \
sbt coverage "test-only org.deepdive.test.integration.ChunkingApp -- -oF"  && \
sbt coverageAggregate

# Running a specific test with Eclipse debugger
# SBT_OPTS="-agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y -Xmx4g" sbt/sbt "test-only org.deepdive.test.unit.PostgresInferenceDataStoreSpec"
