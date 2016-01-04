#!/usr/bin/env bats
load env

setup() {
    cd "${BATS_TEST_FILENAME%.bats}"
    rm -fv ./dd_*_actual.bin
}
teardown() {
    rm -fv ./dd_*_actual.bin
}

@test "format_converter variable works" {
    format_converter variable ./dd_variables.txt ./dd_variables_actual.bin
    cmp ./dd_variables_actual.bin ./dd_variables_expected.bin
}

@test "format_converter factor works" {
    format_converter factor ./dd_factors.txt ./dd_factors_actual.bin 2 1 original 1
    cmp ./dd_factors_actual.bin ./dd_factors_expected.bin
}

@test "format_converter weight works" {
    format_converter weight ./dd_weights.txt ./dd_weights_actual.bin
    cmp ./dd_weights_actual.bin ./dd_weights_expected.bin
}
