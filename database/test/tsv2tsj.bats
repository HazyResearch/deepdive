#!/usr/bin/env bats
# Tests for tsv2tsj
load test_environ

load corner_cases

parse_each_tab_sep_json() {
    tr '\t' '\n' |
    tee /dev/stderr |
    jq -c .
}

@test "tsv2tsj works" {
    cd "$BATS_TEST_DIRNAME"
    actual=$(eval keeping_output_of tsv2tsj $NastyTypes <<<"$NastyTSV" | parse_each_tab_sep_json)
    diff -u <(echo "$NastyTSJ" | jq -c .) \
            <(echo "$actual"   | jq -c .) \
            #
}

@test "tsv2tsj works with null in array" {
    cd "$BATS_TEST_DIRNAME"
    actual=$(eval keeping_output_of tsv2tsj $NullInArrayTypes <<<"$NullInArrayTSV" | parse_each_tab_sep_json)
    diff -u <(echo "$NullInArrayTSJ" | jq -c .) \
            <(echo "$actual"         | jq -c .) \
            #
}

@test "tsv2tsj works with nested array" {
    skip "NOT SUPPORTED YET" # TODO
    cd "$BATS_TEST_DIRNAME"
    actual=$(eval keeping_output_of tsv2tsj $NestedArrayTypes <<<"$NestedArrayTSV" | parse_each_tab_sep_json)
    diff -u <(echo "$NestedArrayTSJ" | jq -c .) \
            <(echo "$actual"         | jq -c .) \
            #
}

@test "tsv2tsj works with unicode" {
    cd "$BATS_TEST_DIRNAME"
    actual=$(eval keeping_output_of tsv2tsj $UnicodeTypes <<<"$UnicodeTSV" | parse_each_tab_sep_json)
    diff -u <(echo "$UnicodeTSJ" | jq -c .) \
            <(echo "$actual"     | jq -c .) \
            #
}

@test "tsv2tsj works with timestamps" {
    skip "NOT SUPPORTED YET" # TODO
    cd "$BATS_TEST_DIRNAME"
    actual=$(eval keeping_output_of tsv2tsj $TimestampTypes <<<"$TimestampTSV" | parse_each_tab_sep_json)
    diff -u <(echo "$TimestampTSJ" | jq -c .) \
            <(echo "$actual"       | jq -c .) \
            #
}
