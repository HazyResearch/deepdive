#!/usr/bin/env bash
set -eu
d=${0%/*}
export LD_LIBRARY_PATH="$d"/../lib/dw/lib/protobuf/lib:"$d"/../lib/dw/lib"${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export DYLD_LIBRARY_PATH="$d"/../lib/dw"${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"
exec "$0"-mac "$@"
