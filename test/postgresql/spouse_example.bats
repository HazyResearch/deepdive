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

@test "$DBVARIANT spouse example (ddlog)" {
    cd ddlog || skip
    deepdive compile
    deepdive redo process/init/db model/calibration-plots
    [[ $(getAccuracyPerCent has_spouse_label) -gt 90 ]]
}

@test "$DBVARIANT spouse example (tsv_extractor)" {
    cd tsv_extractor || skip
    deepdive compile
    deepdive redo process/init/db model/calibration-plots
    [[ $(getAccuracyPerCent has_spouse_is_true) -gt 90 ]] || {
        # XXX mysql fails with some probability, but giving another try usually works
        # TODO fix mysql example to be identical with postgresql
        deepdive compile
        deepdive redo process/init/db model/calibration-plots
        [[ $(getAccuracyPerCent has_spouse_is_true) -gt 90 ]]
    }
}

@test "$DBVARIANT spouse example (json_extractor)" {
    skip
    cd json_extractor || skip
    deepdive compile
    deepdive redo process/init/db model/calibration-plots
    [[ $(getAccuracyPerCent has_spouse_is_true) -gt 90 ]]
}

@test "$DBVARIANT spouse example (piggy_extractor)" {
    skip
    # requires postgres with python support
    [[ $(deepdive sql eval "SELECT COUNT(*) FROM pg_language WHERE lanname = 'plpythonu'") -gt 0 ]] ||
        deepdive sql "CREATE LANGUAGE plpythonu" ||
        skip
    type javac >/dev/null || skip
    cd piggy_extractor || skip
    deepdive compile
    deepdive redo process/init/db model/calibration-plots
    # TODO piggy doesn't have proper inference rules
    # [[ $(getAccuracyPerCent has_spouse_is_true) -gt 90 ]]
}

@test "$DBVARIANT spouse example (plpy_extractor)" {
    skip
    # requires postgres with python support
    [[ $(deepdive sql eval "SELECT COUNT(*) FROM pg_language WHERE lanname = 'plpythonu'") -gt 0 ]] ||
        deepdive sql "CREATE LANGUAGE plpythonu" ||
        skip
    skip # TODO
    cd plpy_extractor || skip
    deepdive compile
    deepdive redo process/init/db model/calibration-plots
    [[ $(getAccuracyPerCent has_spouse_is_true) -gt 90 ]]
}
