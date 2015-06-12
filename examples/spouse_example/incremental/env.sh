#! /bin/bash

export APP_HOME=$PWD
export DEEPDIVE_HOME="$PWD/../../.."
export BASEDIR="$PWD/base"
export PYTHONPATH="$DEEPDIVE_HOME/ddlib:${PYTHONPATH:-}"

# Database Configuration
export DBNAME=deepdive_spouse_inc

export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}
