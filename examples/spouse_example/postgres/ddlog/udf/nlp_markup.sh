#!/usr/bin/env bash
# A shell script that runs Bazaar/Parser over documents passed as input TSV lines
#
# $ deepdive env udf/nlp_markup.sh doc_id _ _ content _
##
set -euo pipefail
cd "$(dirname "$0")"

bazaarParser=bazaar/parser
[[ -x "$bazaarParser"/target/start ]] || {
    echo "# bazaar/parser has not been set up, please run:"
    echo "cd '$PWD' &&"
    echo "git clone https://github.com/HazyResearch/bazaar.git &&"
    echo "cd bazaar/parser &&"
    echo "./setup.sh"
    exit 2
} >&2

[[ $# -gt 0 ]] ||
    # default column order of input TSV
    set -- doc_id content

# convert input tsv lines into JSON lines for Bazaar/Parser
tsv2json "$@" |
# start Bazaar/Parser to emit sentences TSV
"$bazaarParser"/run.sh -i json -k doc_id -v content
