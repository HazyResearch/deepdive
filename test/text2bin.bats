#!/usr/bin/env bats
load helpers

setup() {
    cd "${BATS_TEST_FILENAME%.bats}"
}
teardown() {
    : no-op
}

@test "text2bin variable works" {
    rm -f ./dd_variables.bin
    dw text2bin variable ./dd_variables.txt ./dd_variables.bin /dev/stderr
    compare_binary_with_xxd_text ./dd_variables.bin.txt ./dd_variables.bin
}

@test "text2bin factor works" {
    rm -f ./dd_factors.bin
    dw text2bin factor ./dd_factors.txt ./dd_factors.bin /dev/stderr 2 1 1
    compare_binary_with_xxd_text ./dd_factors.bin.txt ./dd_factors.bin
}

@test "text2bin weight works" {
    rm -f ./dd_weights.bin
    dw text2bin weight ./dd_weights.txt ./dd_weights.bin /dev/stderr
    compare_binary_with_xxd_text ./dd_weights.bin.txt ./dd_weights.bin
}
