#!/usr/bin/env bats
# Tests for spouse example

. "$BATS_TEST_DIRNAME"/env.sh >&2
: ${SUBSAMPLE_NUM_SENTENCES:=3000}
export SUBSAMPLE_NUM_SENTENCES
export DEEPDIVE_CONFIG_EXTRA='
    deepdive.sampler.sampler_args: "-l 500 -i 500 -s 1 --alpha 0.05 --diminish 0.99"
'

# load helpers
. "$BATS_TEST_DIRNAME"/spouse_example.helpers.sh

setup() {
    cd "$BATS_TEST_DIRNAME"/spouse_example
}

@test "$DBVARIANT spouse example" {
    deepdive compile
    # XXX skip testing NLP parser processes  # deepdive redo process/init/db model/calibration-plots
    deepdive redo process/init/app
    deepdive create table sentences
    deepdive load sentences
    deepdive mark done data/sentences
    deepdive redo model/calibration-plots
    [[ $(getAccuracyPerCent has_spouse_label) -gt 90 ]]
}

