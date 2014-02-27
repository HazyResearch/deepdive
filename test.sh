#! /usr/bin/env bash

# Set username and password
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

# Create test database
dropdb deepdive_test
createdb deepdive_test

# Start in-memory SQL server
java -cp lib/hsqldb.jar org.hsqldb.server.Server --database.0 mem:deepdive_test --dbname.0 deepdive_test &

# Run the test
SBT_OPTS="-Xmx2g" sbt "test"

# Kill in-memory SQL server
kill $!