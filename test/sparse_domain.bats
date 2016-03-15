#!/usr/bin/env bats

@test "${BATS_TEST_FILENAME##*/}: Integration test for sparse domain multinomial" {
    # the factor graph used for test is from biased multinomial, which contains 20
    # variables,
    # Variable has cardinality of 4. Evidence variables: 0: 1, 1: 2, 2: 3, 3: 4
    # where the first one is value, the second is count
    # all variables and factors are in sparse domain format
    fg="$BATS_TEST_DIRNAME"/sparse_domain

    # generate factor graph from tsv
    ./text2bin variable "$fg"/variables.tsv "$fg"/graph.variables
    ./text2bin weight   "$fg"/weights.tsv   "$fg"/graph.weights
    ./text2bin factor   "$fg"/factors.tsv   "$fg"/graph.factors 12 1 0 1
    ./text2bin domain   "$fg"/domains.tsv   "$fg"/graph.domains

    # run sampler
    ./dw gibbs \
        -w "$fg"/graph.weights \
        -v "$fg"/graph.variables \
        -f "$fg"/graph.factors \
        -m "$fg"/graph.meta \
        -o "$fg" \
        --domains "$fg"/graph.domains \
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
        # variable 17,18,19 has sparse domains
        expected=(e+1)/10.0;
        if ((id == 17)) {
            if (e == 1) expected=1
            else print "var " id " has a value outside its domain"
        }
        if ((id == 18)) {
            if (e == 0) expected=1/7.0
            else if (e == 1) expected=2/7.0
            else if (e == 3) expected=4/7.0
            else print "var " id " has a value outside its domain"
        }
        if ((id == 19)) {
            if (e == 1) expected=1/3.0
            else if (e == 3) expected=2/3.0
            else print "var " id " has a value outside its domain"
        }
        eps=0.04
        if ((prob > expected + eps) || (prob < expected - eps)) {
            print "var " id " category " e " prob is " prob ", not near " expected
            exit(1)
        }
    }'
}
