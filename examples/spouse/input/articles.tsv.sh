#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

corpus=signalmedia/signalmedia-1m.jsonl
[[ -e "$corpus" ]] || {
    echo "ERROR: Missing $PWD/$corpus"
    echo "# Please Download it from http://research.signalmedia.co/newsir16/signal-dataset.html"
    echo
    echo "# Alternatively, use our sampled data by running:"
    echo "deepdive load articles input/articles-100.tsv.bz2"
    echo
    echo "# Or, skipping all NLP markup processes by running:"
    echo "deepdive create table sentences"
    echo "deepdive load sentences"
    echo "deepdive mark done sentences"
    false
} >&2

cat "$corpus" |
#grep -E 'wife|husband|married' |
#head -100 |
jq -r '[.id, .content] | @tsv'
