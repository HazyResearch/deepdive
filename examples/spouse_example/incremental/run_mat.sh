#!/usr/bin/env bash
# A script for running materialization phase
set -eux
cd "$(dirname "$0")"
. env.sh

export DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES="has_spouse"
export DEEPDIVE_ACTIVE_INCREMENTAL_RULES="has_spouse_0"

./run.sh spouse_example.ddl --materialization initdb      -o "$BASEDIR"
./run.sh spouse_example.ddl --materialization extraction  -o "$BASEDIR"
./run.sh spouse_example.ddl --materialization inference   -o "$BASEDIR"
