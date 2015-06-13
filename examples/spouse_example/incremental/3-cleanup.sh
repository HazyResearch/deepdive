#!/usr/bin/env bash
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-spouse_example.f2.ddl}
Out=${2:-inc-delta.out}

export BASEDIR=$Out

./run.sh "$DDlog" --incremental cleanup "$Out"
