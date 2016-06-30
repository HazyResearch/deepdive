#!/usr/bin/env bash
# Run CoreNLP over documents passed as tab-separated JSONs input
#
# $ deepdive env udf/nlp_markup.sh
##
set -euo pipefail
cd "$(dirname "$0")"

deepdive corenlp start
deepdive corenlp parse-tsj docid+ content=nlp -- docid nlp |
deepdive corenlp sentences-tsj docid nlp \
                            -- docid nlp.{index,word,lemma,pos,ner,characterOffsetBegin,dep,governor}

jq -r '
# TODO doc metadata
[ .sentences[] |
[ [.tokens[].word]
, [.tokens[].lemma]
, [.tokens[].pos]
, [.tokens[] | [.characterOffsetBegin,.characterOffsetEnd]]
] | map(@json) | join("\t")
'

# doc table
jq -R -r '
def withtsv(f) : split("\t") | f | join("\t");
def withjson(f):    fromjson | f | tojson    ;

withtsv(
    [ .[0]
    , .[1] | withjson(
            .sentences[] |
                [ [.tokens[].word]
                , [.tokens[].lemma]
                , [.tokens[].pos]
                , [.tokens[] | [.characterOffsetBegin,.characterOffsetEnd]]
                ]
        )
    ]
)'

# sentences table
jq -R -r '
def withtsv(f) : split("\t") | f | join("\t");
def withjson(f):    fromjson | f | tojson    ;

withtsv(
    . as $row |
    .[1] | (fromjson | .sentences[] |
                [$row[0]] +
                [ [.tokens[].word]
                , [.tokens[].lemma]
                , [.tokens[].pos]
                , [.tokens[] | [.characterOffsetBegin,.characterOffsetEnd]]
                | tojson]                                                  
             )           
              
)'
deepdive corenlp stop
