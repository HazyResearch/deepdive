#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

cat /dfs/scratch0/thodrek/signalmedia/signalmedia-1m.jsonl |
grep -E 'wife|husband|married' |
head -100 |
jq -r '[.id, .content | gsub("[\r]"; "")] | @tsv'
