#!/usr/bin/env bats
# Tests for biased coin example

load test_environ

@test "$DBVARIANT biased coin example" {
    cd "$BATS_TEST_DIRNAME"/biased_coin_example || skip
    deepdive compile
    deepdive redo process/init/app data/model/{weights,probabilities}

    # weight should be around ~= 1.0
    num_unexpected_weights=$(deepdive sql eval "
            SELECT *
            FROM dd_inference_result_weights
            WHERE weight <= 0.9 OR weight >= 1.1
        " | tee /dev/stderr | wc -l)
    echo num_unexpected_weights=$num_unexpected_weights
    [[ $num_unexpected_weights -eq 0 ]]
    # probability should be around 8/9
    num_unexpected_prob=$(deepdive sql eval "
            SELECT *
            FROM dd_inference_result_variables
            WHERE expectation > 0.94 OR expectation < 0.84
        " | tee /dev/stderr | wc -l)
    echo num_unexpected_prob=$num_unexpected_prob
    [[ $num_unexpected_prob -eq 0 ]]
}
