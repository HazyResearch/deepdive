#!/usr/bin/env bats

@test "${BATS_TEST_FILENAME##*/}: Integration test for multinomial" {
    # the factor graph used for test is from biased multinomial, which contains 20
    # variables,
    # Variable has cardinality of 4. Evidence variables: 0: 1, 1: 2, 2: 3, 3: 4
    # where the first one is value, the second is count
    fg="$BATS_TEST_DIRNAME"/multinomial

    # generate factor graph from tsv
    ./text2bin variable "$fg"/variables.tsv "$fg"/graph.variables
    ./text2bin weight   "$fg"/weights.tsv   "$fg"/graph.weights
    ./text2bin factor   "$fg"/factors.tsv   "$fg"/graph.factors 5 1 0 1

    # run sampler
    ./dw gibbs \
        -w "$fg"/graph.weights \
        -v "$fg"/graph.variables \
        -f "$fg"/graph.factors \
        -m "$fg"/graph.meta \
        -o "$fg" \
        -l 4000 -i 2000 -s 1 --alpha 0.01 --diminish 0.999 --reg_param 0

    # check results
    cd "$fg"

    # exponenents of weights should have ratio like 1:2:3:4
    # weight 0 is fixed at 0
    log () {
        python -c "import math; print math.log($1)"
    }
    awk <inference_result.out.weights.text '{
        id=$1; weight=$2;
        # exp_weight=exp(weight);
        expected=log(id+1);
        eps=0.2
        if ((weight > expected+eps) || (weight < expected-eps)) {
            print "exp of weight " id " not near " expected
            exit(1)
        }
    }'

    awk <inference_result.out.text '{
        id=$1; e=$2; prob=$3;
        expected=(e+1)/10.0;
        eps=0.04
        if ((prob > expected + eps) || (prob < expected - eps)) {
            print "var " id " category " e " prob is " prob ", not near " expected
            exit(1)
        }
    }'
}
