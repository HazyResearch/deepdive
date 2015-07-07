#!/usr/bin/env bats
# Tests for biased coin example

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT biased coin example" {
    cd "$BATS_TEST_DIRNAME"/biased_coin_example || skip
    deepdive initdb
    deepdive run

    # weight should be around log(#positive) / log(#negative) ~= 2.1
    [[ $(deepdive sql eval "
            select count(*)
            from dd_inference_result_weights
            where weight <= 1.9 or weight >= 2.3
        ") -eq 0 ]]
    # probability should be around 8/9
    [[ $(deepdive sql eval "
            select count(*)
            from dd_inference_result_variables
            where expectation > 0.94 or expectation < 0.84
        ") -eq 0 ]]
}
