#!/usr/bin/env bash
set -eu
shopt -s nullglob
cd "$(dirname "$0")"

# make sure bats is available
PATH="$PWD/bats/bin:$PATH"

# instantiate bats tests templates under its directory
for t in *.bats.template; do
    [[ -e "$t" ]] || continue
    testSpecDir=${t%.bats.template}
    rm -f "$testSpecDir"/*.bats
    # create a .bats symlink for each test specification
    for testSpec in "$testSpecDir"/*; do
        [[ -d "$testSpec" ]] || continue
        testSpec=${testSpec%/input.ddl}
        batsFile="$testSpec".bats
        ln -sfn ../"$t" "$batsFile"
    done
done

# run all .bats tests
bats "$@" *.bats */*.bats
