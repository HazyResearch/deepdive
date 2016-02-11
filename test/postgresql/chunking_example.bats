#!/usr/bin/env bats
# Tests for chunking example

. "$BATS_TEST_DIRNAME"/env.sh >&2
: ${SUBSAMPLE_NUM_WORDS_TRAIN:=5000} ${SUBSAMPLE_NUM_WORDS_TEST:=1000}
export SUBSAMPLE_NUM_WORDS_TRAIN SUBSAMPLE_NUM_WORDS_TEST

get_f1score() {
    printf '%.0f' $(
        result/eval.sh | tee /dev/stderr | sed -n '/^accuracy:/ s/.* FB1: *//p')
}

@test "$DBVARIANT chunking example" {
    cd "$BATS_TEST_DIRNAME"/chunking_example || skip
    DEEPDIVE_CONFIG_EXTRA='deepdive.calibration.holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM tag WHERE word_id > '${SUBSAMPLE_NUM_WORDS_TRAIN}'"' \
    deepdive compile
    deepdive model weights init
    deepdive redo process/init/app data/model/probabilities
    f1score=$(get_f1score)
    echo "f1score = $f1score"
    [[ $f1score -ge 78 ]]
}

@test "$DBVARIANT chunking example reuse weights" {
    cd "$BATS_TEST_DIRNAME"/chunking_example || skip
    # keep the learned weights from a small corpus
    deepdive model weights keep

    # load larger test corpus
    SUBSAMPLE_NUM_WORDS_TRAIN=0 SUBSAMPLE_NUM_WORDS_TEST=5000 \
    deepdive redo words_raw

    # reuse the weights (to skip learning)
    deepdive model weights reuse  # taking care of all extraction and grounding for larger data

    # and do inference
    deepdive redo data/model/probabilities

    # check quality
    f1score=$(get_f1score)
    echo "f1score = $f1score"
    [[ $f1score -ge 75 ]]
}
