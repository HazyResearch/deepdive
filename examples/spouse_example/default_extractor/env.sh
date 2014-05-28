#! /bin/bash

# Database Configuration
export DBNAME=deepdive_spouse_default
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}
