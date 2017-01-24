#!/usr/bin/env bash

# put source tree root on PATH
PATH="${BASH_SOURCE%/test/helpers.bash}:${BASH_SOURCE%/helpers.bash}:$PATH"

compare_binary_with_xxd_text() {
    local expected_xxd_txt=$1
    local actual_bin=$2
    if ! cmp <(xxd -r "$expected_xxd_txt") "$actual_bin"; then
        diff -u "$expected_xxd_txt" <(xxd "$actual_bin")
        false
    fi
}
