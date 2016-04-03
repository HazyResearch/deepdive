#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"/..

: ${CORENLP_ARCHIVE:=stanford-corenlp-full-2015-12-09.zip}
: ${CORENLP_BASEURL:=http://nlp.stanford.edu/software/}
: ${CORENLP_SHA1SUM:=55c9d653eb30ace7d5a60eb3138e424f9442e539}

echo "Ensuring CoreNLP is installed..."
mkdir -p udf/corenlp
cd udf/corenlp
# download CoreNLP
fetch-verify "$CORENLP_ARCHIVE" \
    "$CORENLP_BASEURL$CORENLP_ARCHIVE" \
    sha1sum=$CORENLP_SHA1SUM
    #
# unzip
[[ -x corenlp/corenlp.sh ]] || {
    rm -rf corenlp
    unzip -o "$CORENLP_ARCHIVE"
    ln -sfnv "${CORENLP_ARCHIVE%.zip}" corenlp
}
