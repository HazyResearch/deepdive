#!/usr/bin/env bash
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-spouse_example.f1.ddl}
Out=${2:-inc-delta.out}
ActiveVarsFile=${3:-${DDlog%.ddl}.active.vars}
ActiveRulesFile=${4:-${DDlog%.ddl}.active.rules}

export BASEDIR=$Out
export DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES="$(sed 's/#.*$//' "$ActiveVarsFile" )"
export DEEPDIVE_ACTIVE_INCREMENTAL_RULES="$(    sed 's/#.*$//' "$ActiveRulesFile")"

./run.sh "$DDlog" --merge extraction "$Out"
./run.sh "$DDlog" --materialization inference "$Out"
