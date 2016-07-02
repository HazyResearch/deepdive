#!/usr/bin/env bash
# Parse documents in tab-separated JSONs input stream with CoreNLP
#
# $ deepdive corenlp install
# $ deepdive corenlp start
# $ deepdive env udf/nlp_markup.sh
# $ deepdive corenlp stop
##
set -euo pipefail
cd "$(dirname "$0")"

# some configuration knobs for CoreNLP
: ${CORENLP_PORT:=$(deepdive corenlp unique-port)}  # a CoreNLP server started ahead of time is shared across parallel UDF processes
# See: http://stanfordnlp.github.io/CoreNLP/annotators.html
: ${CORENLP_ANNOTATORS:="
        tokenize
        ssplit
        pos
        ner
        lemma
        depparse
    "}
export CORENLP_PORT
export CORENLP_ANNOTATORS

# make sure CoreNLP server is available
deepdive corenlp is-running || {
    echo >&2 "PLEASE MAKE SURE YOU HAVE RUN: deepdive corenlp start"
    false
}

# parse input with CoreNLP and output a row for every sentence
deepdive corenlp parse-tsj docid+ content=nlp -- docid nlp |
deepdive corenlp sentences-tsj docid content:nlp \
                            -- docid nlp.{index,tokens.{word,lemma,pos,ner,characterOffsetBegin}} \
                                     nlp.collapsed-dependencies.{dep_type,dep_token}
