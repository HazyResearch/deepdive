#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
if [[ -n ${SUBSAMPLE_NUM_SENTENCES:-} ]]; then
    bzcat ./sentences_dump.csv.bz2 | head -n ${SUBSAMPLE_NUM_SENTENCES}
else
    bzcat ./sentences_dump.csv.bz2
fi
