#! /usr/bin/env bash

# Set username and password
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-}

# Create test database
dropdb deepdive_test
createdb deepdive_test

# Run the test
SBT_OPTS="-Xmx2g" sbt "test"

# Drop test database
dropdb deepdive_test