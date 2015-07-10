#!/usr/bin/env bash
# A script for enumerating all .bats files after instantiating any templates
set -eu

: ${TEST_ROOT:=test}

( cd "$(dirname "$0")"

# instantiate bats tests templates under its directory
for t in *.bats.template; do
    [[ -e "$t" ]] || continue
    testSpecDir=${t%.bats.template}
    rm -f "$testSpecDir"/*.bats
    # create a .bats symlink for each test specification
    for testSpec in "$testSpecDir"/*; do
        [[ -d "$testSpec" ]] || continue
        batsFile="$testSpec".bats
        ln -sfn ../"$(basename "$t")" "$batsFile"
        echo "$TEST_ROOT/$batsFile"
    done
done
)

ls "$TEST_ROOT"/*.bats 2>/dev/null
