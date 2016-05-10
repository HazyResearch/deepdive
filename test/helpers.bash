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
    local fg=$1; shift
    cd "$fg"

    # generate factor graph from tsv
    for what in variable domain factor weight; do
        for tsv in "$what"s*.tsv; do
            [[ -e "$tsv" ]] || continue
            text2bin "$what" "$tsv" graph."${tsv%.tsv}" $(
                # use extra text2bin args if specified
                ! [[ -e "${tsv%.tsv}".text2bin-opts ]] || cat "${tsv%.tsv}".text2bin-opts
            )
        done
    done

    # run sampler
    dw gibbs \
        -w <(cat graph.weights*) \
        -v <(cat graph.variables*) \
        -f <(cat graph.factors*) \
        -m graph.meta \
        -o . \
        --domains <(cat graph.domains*) \
        "$@"
}
