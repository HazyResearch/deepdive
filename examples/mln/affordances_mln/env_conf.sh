#! /bin/bash

# Database Configuration
export DBNAME=deepdive_affordances_mln
export PGUSER=${PGUSER:-`whoami`}
# Password must not be empty for Tuffy port to run
export PGPASSWORD=${PGPASSWORD:-1234}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

