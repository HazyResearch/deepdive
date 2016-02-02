#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

corpus=signalmedia/signalmedia-1m.jsonl
[[ -e "$corpus" ]] || {
    echo "Missing $PWD/$corpus"
    echo "Please Download it from http://research.signalmedia.co/newsir16/signal-dataset.html"
    false
} >&2

cat "$corpus" |
grep -E 'wife|husband|married' |
head -100 |
jq -r '[.id, .content | gsub("[\r]"; "")] | @tsv'
