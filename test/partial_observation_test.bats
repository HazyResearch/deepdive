#!/usr/bin/env bats

@test "${BATS_TEST_FILENAME##*/}: Integration test for learning hidden models" {
    # The factor graph contains a chain A-B-C, where A, C are evidence, and B
    # is partially observed. The factor between A, B and B, C is equal,
    # respectively.  There are 4 training examples with A = C = true, and only
    # one B is observed to be true. We expect the weight to be positive, and
    # probabilities to be > 0.9.
    fg="$BATS_TEST_DIRNAME"/partial

    # generate factor graph from tsv
    ./text2bin variable "$fg"/variables.tsv "$fg"/graph.variables
    ./text2bin weight   "$fg"/weights.tsv   "$fg"/graph.weights
    ./text2bin factor   "$fg"/factors.tsv   "$fg"/graph.factors 3 2 0 1 1

    # run sampler
    ./dw gibbs \
        -w "$fg"/graph.weights \
        -v "$fg"/graph.variables \
        -f "$fg"/graph.factors \
        -m "$fg"/graph.meta \
        -o "$fg" \
        -l 300 -i 500 -s 1 --alpha 0.1 --learn_non_evidence

    # check results
    cd "$fg"

    # all weights should be positive
    awk <inference_result.out.weights.text '{
        id=$1; weight=$2;
        if (!(weight > 0)) {
            print "weight " id " <= " 0
            exit(1)
        }
    }'

    # all probabilities should be > 0.9
    awk <inference_result.out.text '{
        id=$1; e=$2; prob=$3;
        if (!(prob > 0.9)) {
            print "var " id " has prob <= " 0.9
            exit(1)
        }
    }'
}
