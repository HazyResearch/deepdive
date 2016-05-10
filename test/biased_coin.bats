#!/usr/bin/env bats
load helpers

@test "${BATS_TEST_FILENAME##*/}: Integration test for learning biased coins" {
    # the factor graph used for test is from biased coin, which contains 18
    # variables,
    # 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
    # positive and id 8 negative.

    run_end_to_end "${BATS_TEST_FILENAME%.bats}" -l 2000 -i 2000 -s 1 --alpha 0.1 --diminish 0.995 --sample_evidence --reg_param 0

    # check results

    # all weights should be around 2.1
    awk <inference_result.out.weights.text '{
        id=$1; weight=$2;
        expected=2.1
        eps=0.1
        if ((weight > expected + eps) || (weight < expected - eps)) {
            print "weight " id " not around " 2.1
            exit(1)
        }
    }'

    # all probabilities should be around 0.89
    awk <inference_result.out.text '{
        id=$1; e=$2; prob=$3;
        expected=0.89
        eps=0.03
        if ((prob > expected + eps) || (prob < expected - eps)) {
            print "var " id " prob not near " 0.89
            exit(1)
        }
    }'
}
