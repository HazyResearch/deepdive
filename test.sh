#! /usr/bin/env bash

# Set username and password
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

# Create test database
dropdb deepdive_test
createdb deepdive_test

# Run the test
SBT_OPTS="-Xmx2g" sbt "test"