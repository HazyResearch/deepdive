#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"

bzcat senna.filtered.tsv.bz2
