#!/usr/bin/env bash
# A shell script that runs Bazaar/Parser over documents passed as input TSV lines
#
# $ deepdive env udf/nlp_markup.sh doc_id _ _ content _
##
set -euo pipefail
cd "$(dirname "$0")"

: ${BAZAAR_HOME:=$PWD/bazaar}
[[ -x "$BAZAAR_HOME"/parser/target/start ]] || {
    echo "No Bazaar/Parser set up at: $BAZAAR_HOME/parser"
    exit 2
} >&2

[[ $# -gt 0 ]] ||
    # default column order of input TSV
    set -- doc_id content

# convert input tsv lines into JSON lines for Bazaar/Parser
tsv2json "$@" |
# start Bazaar/Parser to emit sentences TSV
"$BAZAAR_HOME"/parser/run.sh -i json -k doc_id -v content |
# finally, fixup unescaped characters in the TSV emitted by Bazaar/Parser
# (This will become unnecessary once HazyResearch/bazaar#20 is fixed)
# See: http://www.postgresql.org/docs/9.5/static/sql-copy.html#AEN74312
sed -e "$(for ch in b f r v; do printf 's/\'$ch'/\\\\'$ch'/g;'; done)"
