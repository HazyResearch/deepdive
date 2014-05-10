#! /bin/bash

export APP_HOME=`pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../..; pwd`

# Database Configuration
export DBNAME=deepdive_tables
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

# SBT Options
export MEMORY="4g"
export SBT_OPTS="-Xmx$MEMORY"