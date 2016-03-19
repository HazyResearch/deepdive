#!/usr/bin/env bash

# put source tree root on PATH
PATH="${BASH_SOURCE%/test/helpers.bash}:$PATH"

compare_binary_with_xxd_text() {
    local expected_xxd_txt=$1
    local actual_bin=$2
    if ! cmp <(xxd -r "$expected_xxd_txt") "$actual_bin"; then
        diff -u "$expected_xxd_txt" <(xxd "$actual_bin")
        false
    fi
}

# convert the factor graph from tsv to binary format
# and run the sampler
run_end_to_end() {
    local fg=$1
    local text2bin_factor_args=$2
    local sampler_arg=$3

    # generate factor graph from tsv
    ./text2bin variable "$fg"/variables.tsv "$fg"/graph.variables
    ./text2bin weight   "$fg"/weights.tsv   "$fg"/graph.weights
    ./text2bin factor   "$fg"/factors.tsv   "$fg"/graph.factors $text2bin_factor_args
    ./text2bin domain   "$fg"/domains.tsv   "$fg"/graph.domains

    # run sampler
    ./dw gibbs \
        -w "$fg"/graph.weights \
        -v "$fg"/graph.variables \
        -f "$fg"/graph.factors \
        -m "$fg"/graph.meta \
        -o "$fg" \
        --domains "$fg"/graph.domains \
        $sampler_arg
}