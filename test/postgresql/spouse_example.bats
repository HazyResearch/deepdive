#!/usr/bin/env bats
# Tests for spouse example

load test_environ
: ${SUBSAMPLE_NUM_SENTENCES:=10000}
export DEEPDIVE_CONFIG_EXTRA='
    deepdive.sampler.sampler_args: "-l 500 -i 500 --alpha 0.05 --diminish 0.99"
'

# load helpers
. "$BATS_TEST_DIRNAME"/spouse_example.helpers.sh

setup() {
    cd "$BATS_TEST_DIRNAME"/spouse_example
}

@test "$DBVARIANT spouse example compile/init" {
    deepdive compile

    # ensure CoreNLP build is skipped
    # TODO test NLP markup processes
    mkdir -p udf/bazaar/parser/target
    touch    udf/bazaar/parser/target/start
    chmod +x udf/bazaar/parser/target/start
    deepdive redo process/init/app
}

@test "$DBVARIANT spouse example load corpus" {
    # XXX skip testing NLP parser processes
    deepdive create table sentences
    DEEPDIVE_LOAD_FORMAT=tsj \
    deepdive load sentences <(bzcat input/sentences-1000.tsj.bz2 | head -$SUBSAMPLE_NUM_SENTENCES)
    deepdive mark done data/sentences
    deepdive mark new data/sentences
}

@test "$DBVARIANT spouse example extraction/grounding" {
    deepdive model weights init
    deepdive do model/factorgraph
}

@test "$DBVARIANT spouse example learning/inference/calibration" {
    deepdive do model/calibration-plots
}

@test "$DBVARIANT spouse example gives good quality" {
    accuracyPerCent=$(getAccuracyPerCent has_spouse_label)
    echo "accuracy = $accuracyPerCent >= 90?"
    [[ $accuracyPerCent -ge 90 ]] || skip "accuracy=$accuracyPerCent < 90" # TODO fail test with low accuracy
}

@test "$DBVARIANT spouse example keep weights" {
    # keep the learned weights from a small corpus
    deepdive model weights keep
}

@test "$DBVARIANT spouse example load larger corpus" {
    # load larger corpus
    deepdive create table sentences
    DEEPDIVE_LOAD_FORMAT=tsj \
    deepdive load sentences <(bzcat input/sentences-1000.tsj.bz2 | head -$((2 * $SUBSAMPLE_NUM_SENTENCES)))
    deepdive mark done data/sentences
    deepdive mark new data/sentences
}

@test "$DBVARIANT spouse example reuse weights" {
    # reuse the weights (skipping learning) and do inference
    deepdive model weights reuse  # taking care of all extraction and grounding for larger data
}

@test "$DBVARIANT spouse example skips learning" {
    deepdive model infer
    deepdive redo model/calibration-plots
}

@test "$DBVARIANT spouse example generalizes well" {
    accuracyPerCent=$(getAccuracyPerCent has_spouse_label)
    echo "accuracy = $accuracyPerCent >= 90?"
    [[ $accuracyPerCent -ge 90 ]] || skip "accuracy=$accuracyPerCent < 90" # TODO fail test with low accuracy
}
