#! /usr/bin/env bash
# Print every line
set -x

# Set username and password
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}
export DBNAME=deepdive_test
export PGDATABASE=$DBNAME  # for testing to work with null settings
export GPHOST=${GPHOST:-localhost}
export GPPORT=${GPPORT:-15433}
export GPPATH=${GPPATH:-/tmp}
export GPLOAD=true
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

# Launch gpfdist if not launched.
gpfdist -d $GPPATH -p $GPPORT &

cd $DEEPDIVE_HOME/lib

case $(uname) in
  Darwin)
    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac/lib/protobuf/lib:$DEEPDIVE_HOME/lib/dw_mac/lib:$LD_LIBRARY_PATH
    export DYLD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac:$DYLD_LIBRARY_PATH
    ;;

  Linux*)
    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/lib/dw_linux/lib64:$DEEPDIVE_HOME/lib/dw_linux/lib/numactl-2.0.9/:$LD_LIBRARY_PATH
    ;;

  *)
    echo >&2 "$(uname): Unsupported OS"
    false
esac

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
# sbt coverage "test-only org.deepdive.test.integration.ChunkingApp -- -oF" && \
sbt coverageAggregate

# Running a specific test with Eclipse debugger
# SBT_OPTS="-agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y -Xmx4g" sbt/sbt "test-only org.deepdive.test.unit.PostgresInferenceDataStoreSpec"
