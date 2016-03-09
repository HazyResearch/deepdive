#!/usr/bin/env bats
load helpers

setup() {
    cd "${BATS_TEST_FILENAME%.bats}"
    rm -fv ./dd_*.bin
}
teardown() {
    rm -fv ./dd_*.bin
}

@test "text2bin variable works" {
    text2bin variable ./dd_variables.txt ./dd_variables.bin
    compare_binary_with_xxd_text ./dd_variables.bin.txt ./dd_variables.bin 
}

@test "text2bin factor works" {
    text2bin factor ./dd_factors.txt ./dd_factors.bin 2 1 original 1
    compare_binary_with_xxd_text ./dd_factors.bin.txt ./dd_factors.bin 
}

@test "text2bin weight works" {
    text2bin weight ./dd_weights.txt ./dd_weights.bin
    compare_binary_with_xxd_text ./dd_weights.bin.txt ./dd_weights.bin 
}
