#!/usr/bin/env bash
set -eu
d=${0%/*}
export LD_LIBRARY_PATH="$d"/../lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
exec "$0".bin "$@"
