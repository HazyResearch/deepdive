#!/usr/bin/env bash
set -eu
d=${0%/*}
export LD_LIBRARY_PATH="$d"/../lib/dw_linux/lib:"$d"/../lib/dw_linux/lib64:"$d"/../lib/dw_linux/lib/numactl-2.0.9${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
exec "$0"-linux "$@"
