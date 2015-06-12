#!/usr/bin/env bash
# A script for running materialization phase
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-$PWD/spouse_example.ddl}
ActiveVarsFile=${2:-${DDlog%.ddl}.active.vars}
ActiveRulesFile=${3:-${DDlog%.ddl}.active.rules}

export DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES="$(cat "$ActiveVarsFile")"
export DEEPDIVE_ACTIVE_INCREMENTAL_RULES="$(cat "$ActiveRulesFile")"

./run.sh "$DDlog" --materialization extraction  -o "$BASEDIR"
./run.sh "$DDlog" --materialization inference   -o "$BASEDIR"
