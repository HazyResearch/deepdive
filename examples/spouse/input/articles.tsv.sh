#!/usr/bin/env bash
cd "$(dirname "$0")"

cat /dfs/scratch0/thodrek/signalmedia/signalmedia-1m.jsonl |
grep -E 'wife|husband|married' |
head -100 |
jq -r '[.id, .content] | @tsv'
