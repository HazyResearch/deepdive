#!/usr/bin/env bash
# A script for loading data for DeepDive's Chunking example
set -eu
cd "$(dirname "$0")"

deepdive sql execute "copy words_raw(word, pos, tag) from STDIN delimiter ' ' null 'null'" <./train_null_terminated.txt
deepdive sql execute "copy words_raw(word, pos, tag) from STDIN delimiter ' ' null 'null'" <./test_null_terminated.txt
