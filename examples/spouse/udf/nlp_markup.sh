#!/usr/bin/env bash
# Run CoreNLP over documents passed as tab-separated JSONs input
#
# $ deepdive env udf/nlp_markup.sh
##
set -euo pipefail
cd "$(dirname "$0")"

: ${CORENLP_BASEPORT:=9000}  # TODO randomize per $USER
: ${CORENLP_JAVAOPTS:=}
: ${CORENLP_HOME:=$PWD/corenlp}

# ensure CoreNLP is installed
[[ -x "$CORENLP_HOME"/corenlp/corenlp.sh ]] || {
    echo "No CoreNLP installed at: $CORENLP_HOME"
    exit 2
} >&2

# Start CoreNLP server
port=$(($CORENLP_BASEPORT + ${DEEPDIVE_CURRENT_PROCESS_INDEX:-0}))
java ${CORENLP_JAVAOPTS} -cp "$CORENLP_HOME/corenlp/*" edu.stanford.nlp.pipeline.StanfordCoreNLPServer --port $port &
corenlp_server_pid=$!
trap 'kill -TERM $corenlp_server_pid' EXIT

# CoreNLP properties
annotators=(
    tokenize
    ssplit
    pos
    lemma
    depparse
)
corenlp_properties=$(echo '
{ "outputFormat"        : "json"
, "annotators"          : "'"$(IFS=,; echo "${annotators[*]}")"'"
, "tokenize.whitespace" : "false"
}')
corenlp_server_endpoint="http://localhost:$port/?properties= $(jq -r '@uri' <<<"$corenlp_properties")"
echo "corenlp_server_endpoint=$corenlp_server_endpoint"

# wait for CoreNLP server to boot up
while ! curl -fsSL --globoff "$corenlp_server_endpoint" --data-raw 'foo' >/dev/null
do sleep 0.$RANDOM; done

# parse each input TSJ line and have CoreNLP server process it over HTTP
tsj-to-corenlp_http_api_reqs() {
perl -e '
use strict;
use feature q(say);
use LWP::UserAgent;
use JSON;
my $ua = LWP::UserAgent->new;
my $req = HTTP::Request->new( POST => shift @ARGV );
my $json = JSON->new->allow_nonref;
while ( <STDIN> ) {
    # parse input document id and content line in TSJ
    chomp; my ($json_doc_id, $json_content) = split "\t";
    say STDERR "Parsing $json_doc_id";
    $req->content( $json->decode($json_content) );
    # send HTTP request
    my $resp = $ua->request($req);
    if ($resp->is_success) {
        # and output result
        my $json_result = $resp->decoded_content;
        # TODO project $json_result fields into separate columns
        print join("\t", $json_doc_id, $json_result), "\n";
    } else {
        # or show error message
        say STDERR "CoreNLP server ERROR ", $resp->code, ": ", $resp->message;
    }
}
' "$@"
}
tsj-to-corenlp_http_api_reqs "$corenlp_server_endpoint" | #docid content -- docid content.nlp

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
