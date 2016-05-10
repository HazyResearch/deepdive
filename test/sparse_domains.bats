#!/usr/bin/env bats
load helpers

@test "${BATS_TEST_FILENAME##*/}: Integration test for sparse domain multinomial" {
    # the factor graph used for test is from biased multinomial, which contains 20
    # variables,
    # Variable has cardinality of 4. Evidence variables: 0: 1, 1: 2, 2: 3, 3: 4
    # where the first one is value, the second is count
    # all variables and factors are in sparse domain format
    fg=${BATS_TEST_FILENAME%.bats}

    # prepare binary factors
    text2bin factor   "$fg"/factors2.tsv   "$fg"/graph.factors2 12 2 0 1 1

    run_end_to_end $fg "12 1 0 1" "-l 4000 -i 2000 -s 1 --alpha 0.01 --diminish 0.999 --reg_param 0"

    # check results
    cd "$fg"

    # exponenents of weights should have ratio like 1:2:3:4
    # weight 0 is fixed at 0
    awk <inference_result.out.weights.text '{
        id=$1; weight=$2;
        if (id < 4) {
            expected=log(id+1);
            eps=0.2
            if ((weight > expected+eps) || (weight < expected-eps)) {
                print "weight " id " not near " expected
                exit(1)
            }
        }
    }'

    awk <inference_result.out.text '{
        id=$1; e=$2; prob=$3;
        # variable 17,18,19 has sparse domains
        expected=(e+1)/10.0;
        if ((id == 15)) {
            if (e == 1) expected=1
            else print "var " id " has a value outside its domain"
        }
        if ((id == 14)) {
                 if (e == 0) expected=(exp(2)/(exp(2)+exp(3)+1))
            else if (e == 1) expected=(exp(3)/(exp(2)+exp(3)+1))
            else if (e == 3) expected=(     1/(exp(2)+exp(3)+1))
            else print "var " id " has a value outside its domain"
        }
        if ((id == 16)) {
                 if (e == 1) expected=(exp(3)/(exp(2)+exp(3)))
            else if (e == 3) expected=(exp(2)/(exp(2)+exp(3)))
            else print "var " id " has a value outside its domain"
        }
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
