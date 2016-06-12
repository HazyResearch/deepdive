#!/usr/bin/env bats
# Tests for deepdive relation
. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "deepdive relation usage" {
    ! deepdive relation || false
    ! deepdive relation columns || false
}

@test "deepdive relation" {
    cd "$BATS_TMPDIR"
    mkdir -p not-an-app
    cd not-an-app
    ! deepdive relation list || false
    ! deepdive relation variables || false
    ! deepdive relation columns foo || false
}

@test "deepdive relation spouse example" {
    cd "$DEEPDIVE_SOURCE_ROOT"/examples/spouse
    deepdive compile
    deepdive relation list | diff -u - <(
        echo articles
        echo has_spouse
        echo num_people
        echo person_mention
        echo sentences
        echo spouse_candidate
        echo spouse_feature
        echo spouse_label
        echo spouse_label__0
        echo spouse_label_resolved
        echo spouses_dbpedia
    )
    deepdive relation columns articles | diff -u - <(
        echo id:text
        echo content:text
    )
    deepdive relation columns has_spouse | diff -u - <(
        echo p1_id:text
        echo p2_id:text
        echo label:boolean
    )
}

@test "deepdive relation chunking example" {
    cd "$DEEPDIVE_SOURCE_ROOT"/examples/chunking
    deepdive compile
    deepdive relation list | diff -u - <(
        echo tag
        echo tags
        echo word_features
        echo words
        echo words_raw
    )
    deepdive relation columns words | diff -u - <(
        echo sent_id:bigint
        echo word_id:bigint
        echo word:text
        echo pos:text
        echo true_tag:text
        echo tag:int
    )
}
