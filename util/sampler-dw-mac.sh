#!/usr/bin/env bash
set -eu
d=${0%/*}
export DYLD_LIBRARY_PATH="$d"/../lib"${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"
exec "$0"-mac "$@"
