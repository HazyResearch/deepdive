#!/usr/bin/env bash
# Run CoreNLP over documents passed as tab-separated JSONs input
#
# $ deepdive env udf/nlp_markup.sh
##
set -euo pipefail
cd "$(dirname "$0")"

# some configuration knobs for CoreNLP
: ${CORENLP_PORT:=}      # specify a port to launch only one CoreNLP server or share an already running one
: ${CORENLP_BASEPORT:=}  # specify the base port to launch many CoreNLP servers
: ${CORENLP_JAVAOPTS:=}  # specify any custom JVM flags to use
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
export CORENLP_BASEPORT CORENLP_JAVAOPTS
deepdive corenlp start

export CORENLP_ANNOTATORS
deepdive corenlp parse-tsj docid+ content=nlp -- docid nlp |
deepdive corenlp sentences-tsj docid content:nlp \
                            -- docid nlp.{index,tokens.{word,lemma,pos,ner,characterOffsetBegin}} \
                                     nlp.collapsed-dependencies.{dep_type,dep_token}

[[ -n ${CORENLP_PORT:-} && $DEEPDIVE_CURRENT_PROCESS_INDEX -ne 1 ]] ||
    # stop when it's safe to do so
    deepdive corenlp stop
