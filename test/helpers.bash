#!/usr/bin/env bash

# put source tree root on PATH
PATH="${BASH_SOURCE%/test/helpers.bash}:$PATH"

compare_binary_with_xxd_text() {
    expected_xxd_txt=$1
    actual_bin=$2
    cmp <(xxd -r "$1") "$2"
}
