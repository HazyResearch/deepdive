#!/usr/bin/env bats
# Tests for spouse example

. "$BATS_TEST_DIRNAME"/env.sh >&2
: ${SUBSAMPLE_NUM_SENTENCES:=3000}
export SUBSAMPLE_NUM_SENTENCES

# load helpers
. "$BATS_TEST_DIRNAME"/spouse_example.helpers.sh

setup() {
    example_dir=$DBVARIANT
    case $DBVARIANT in
        postgresql)    example_dir=postgres ;;
        postgresql-xl) example_dir=pgxl ;;
    esac
    cd "$DEEPDIVE_SOURCE_ROOT"/examples/spouse_example/$example_dir
}

@test "$DBVARIANT spouse example (tsv_extractor)" {
    cd tsv_extractor || skip
    deepdive initdb
    deepdive run
    [[ $(getAccuracyPerCent) -gt 90 ]]
}

@test "$DBVARIANT spouse example (json_extractor)" {
    cd json_extractor || skip
    deepdive initdb
    deepdive run
    [[ $(getAccuracyPerCent) -gt 90 ]]
}

@test "$DBVARIANT spouse example (piggy_extractor)" {
    cd piggy_extractor || skip
    deepdive initdb
    deepdive run
    # TODO piggy doesn't have proper inference rules
    # [[ $(getAccuracyPerCent) -gt 90 ]]
}

@test "$DBVARIANT spouse example (plpy_extractor)" {
    skip # TODO
    cd plpy_extractor || skip
    deepdive initdb
    deepdive run
    [[ $(getAccuracyPerCent) -gt 90 ]]
}
