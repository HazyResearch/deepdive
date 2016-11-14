#!/usr/bin/env bats
# Tests for @tsv_extractor Python function/generator decorator
load test_environ

load ../../database/test/corner_cases

@test "Python @tsj_extractor decorator parser/formatter work correctly" {
    cd "$BATS_TEST_DIRNAME"
    # use an identity UDF to see if the nasty input gets parsed correctly and output correctly
    diff -u \
        <(echo "$NastyTSJ"                                                       | jq -c .) \
        <(echo "$NastyTSJ" | deepdive env python ./tsj_extractor_identity_udf.py | jq -c .) \
        #
}

# TODO also cross validate in another format (like JSON) to catch errors made on both ends
