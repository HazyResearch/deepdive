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
awk '
BEGIN {
    SENT_ID=1
    WORD_ID=1
}
{
    if ($1 == "null" && $2 == "null" && $3 == "null" ) {
        SENT_ID++
    } else {
        print SENT_ID, WORD_ID, $0
    }
    WORD_ID++
}
' |
sed 's/ /	/g' |
DEEPDIVE_LOAD_FORMAT=tsv \
deepdive load "words(sent_id, word_id, word, pos, true_tag)" /dev/stdin
