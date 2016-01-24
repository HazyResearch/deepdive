#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

nl --starting-line-number=0 raw/raw-words/feature_names.txt |
# trip leading spaces and turn spaces into a tab
sed 's/^  *//; s/  */\t/'
