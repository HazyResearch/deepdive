#!/usr/bin/env bash
set -eu
d=${0%/*}
export LD_LIBRARY_PATH="$d"/../lib/dw_mac/lib/protobuf/lib:"$d"/../lib/dw_mac/lib"${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export DYLD_LIBRARY_PATH="$d"/../lib/dw_mac"${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"
exec "$0"-mac "$@"
