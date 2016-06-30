#!/usr/bin/env bash
# Run CoreNLP over documents passed as tab-separated JSONs input
#
# $ deepdive env udf/nlp_markup.sh
##
set -euo pipefail
cd "$(dirname "$0")"

# some configuration knobs for CoreNLP
: ${CORENLP_BASEPORT:=}
: ${CORENLP_JAVAOPTS:=}
: ${CORENLP_ANNOTATORS:="
        tokenize
        ssplit
        pos
        ner
        lemma
        depparse
    "}

export CORENLP_BASEPORT CORENLP_JAVAOPTS
deepdive corenlp start

export CORENLP_ANNOTATORS
deepdive corenlp parse-tsj docid+ content=nlp -- docid nlp |
deepdive corenlp sentences-tsj docid content:nlp \
                            -- docid nlp.{index,tokens.{word,lemma,pos,ner,characterOffsetBegin}} \
                                     nlp.collapsed-dependencies.{dep_type,dep_token}

deepdive corenlp stop
