#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
. "$PWD"/helpers.bash

cp -af biased_coin/. biased_coin-performance/
cd biased_coin-performance
run_end_to_end.sh
echo '-l 20000 -i 20000 -s 1 --alpha 0.1 --diminish 0.995 --sample_evidence --reg_param 0' >dw-args
run_end_to_end.sh
