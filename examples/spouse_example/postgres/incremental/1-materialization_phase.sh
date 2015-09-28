#!/usr/bin/env bash
# A script for running materialization phase
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-spouse_example.f1.ddlog}
Out=${2:-inc-base.out}
ActiveVarsFile=${3:-${DDlog%.ddlog}.active.vars}
ActiveRulesFile=${4:-${DDlog%.ddlog}.active.rules}

export BASEDIR=$Out
export DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES="$(sed 's/#.*$//' "$ActiveVarsFile" )"
export DEEPDIVE_ACTIVE_INCREMENTAL_RULES="$(    sed 's/#.*$//' "$ActiveRulesFile")"

./run.sh "$DDlog" --materialization extraction "$Out"
./run.sh "$DDlog" --materialization inference  "$Out"
