#!/usr/bin/env bats

@test "${BATS_TEST_FILENAME##*/}: Integration test for learning biased coins" {
    # the factor graph used for test is from biased coin, which contains 18
    # variables,
    # 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
    # positive and id 8 negative.
    fg="$BATS_TEST_DIRNAME"/coin

    # generate factor graph from tsv
    ./text2bin variable "$fg"/variables.tsv "$fg"/graph.variables
    ./text2bin weight   "$fg"/weights.tsv   "$fg"/graph.weights
    ./text2bin factor   "$fg"/factors.tsv   "$fg"/graph.factors 4 1 0 1

    # run sampler
    ./dw gibbs \
        -w "$fg"/graph.weights \
        -v "$fg"/graph.variables \
        -f "$fg"/graph.factors \
        -m "$fg"/graph.meta \
        -o "$fg" \
        -l 2000 -i 2000 -s 1 --alpha 0.1 --diminish 0.995 --sample_evidence --reg_param 0

    # check results
    cd "$fg"

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
