#!/usr/bin/env bash
# A shell script that runs Bazaar/Parser over documents passed as input TSV lines
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
corenlp_properties=$(IFS=,; echo '
{ "outputFormat"        : "json"
, "annotators"          : "'"${annotators[*]}"'"
, "tokenize.whitespace" : "true"
}')
corenlp_server_endpoint="http://localhost:$port/?properties= $(jq -r '@uri' <<<"$corenlp_properties")"

# wait for CoreNLP server to boot up
while ! curl -fsSL --globoff "$corenlp_server_endpoint" --data-raw 'foo' -o /dev/null
do sleep 0.$RANDOM; done

# parse each input TSJ line and have CoreNLP server process it over HTTP
perl -e '
use strict;
use feature q(say);
use LWP::UserAgent;
use JSON;
my $ua = LWP::UserAgent->new;
my $req = HTTP::Request->new( POST => shift @ARGV );
my $json = JSON->new->allow_nonref;
while ( <> ) {
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
' "$corenlp_server_endpoint"
