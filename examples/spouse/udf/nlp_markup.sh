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
export CORENLP_ANNOTATORS

# XXX due to a performance issue during launch, sharing a single CoreNLP server
# is a better idea unless there's a way to use taskset(1) to limit the impact
type taskset &>/dev/null || CORENLP_PORT=$(deepdive corenlp unique-port)

# start CoreNLP server unless CORENLP_PORT for a shared server was given
[[ -n ${CORENLP_PORT:-} ]] || deepdive corenlp start
deepdive corenlp is-running || {
    echo >&2 "PLEASE MAKE SURE YOU HAVE RUN: deepdive corenlp start $CORENLP_PORT"
    echo >&2 "OR, SPECIFY THE CORRECT PORT: export CORENLP_PORT=$CORENLP_PORT"
    false
}

# parse input with CoreNLP and output a row for every sentence
deepdive corenlp parse-tsj docid+ content=nlp -- docid nlp |
deepdive corenlp sentences-tsj docid content:nlp \
                            -- docid nlp.{index,tokens.{word,lemma,pos,ner,characterOffsetBegin}} \
                                     nlp.collapsed-dependencies.{dep_type,dep_token}

# stop the CoreNLP server unless CORENLP_PORT for a shared server was given
[[ -n ${CORENLP_PORT:-} ]] || deepdive corenlp stop
