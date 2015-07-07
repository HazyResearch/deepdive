#!/usr/bin/env bash
# A script for loading data for DeepDive's Chunking example
set -eu
cd "$(dirname "$0")"

if [[ -n ${SUBSAMPLE_NUM_WORDS_TRAIN:-} && -n ${SUBSAMPLE_NUM_WORDS_TEST:-} ]]; then
    head -n ${SUBSAMPLE_NUM_WORDS_TRAIN} ./train_null_terminated.txt
    head -n ${SUBSAMPLE_NUM_WORDS_TEST}   ./test_null_terminated.txt
else
    cat ./train_null_terminated.txt ./test_null_terminated.txt
fi |
deepdive sql "copy words_raw(word, pos, tag) from STDIN delimiter ' ' null 'null'"
