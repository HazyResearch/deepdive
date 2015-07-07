#!/usr/bin/env bats
# Tests for chunking example

. "$BATS_TEST_DIRNAME"/env.sh >&2
: ${SUBSAMPLE_NUM_WORDS_TRAIN:=5000} ${SUBSAMPLE_NUM_WORDS_TEST:=1000}
export SUBSAMPLE_NUM_WORDS_TRAIN SUBSAMPLE_NUM_WORDS_TEST

@test "$DBVARIANT chunking example" {
    cd "$BATS_TEST_DIRNAME"/chunking_example || skip
    deepdive initdb
    DEEPDIVE_CONFIG_EXTRA='deepdive.calibration.holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM words WHERE word_id > '${SUBSAMPLE_NUM_WORDS_TRAIN}'"' \
    deepdive run

    f1score=$(printf '%.0f' $(
        result/eval.sh | tee /dev/stderr | sed -n '/^accuracy:/ s/.* FB1: *//p'))
    [[ $f1score -ge 80 ]]
}
