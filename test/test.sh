#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"

# make sure bats is available
PATH="$PWD/bats/bin:$PATH"
type bats &>/dev/null ||
    git clone https://github.com/sstephenson/bats.git

# generate bats tests for every per-example templates
for t in *.bats.per-example; do
    testName=${t%.bats.per-example}
    # generate one for each example
    for ddl in ../examples/*.ddl; do
        exampleName=${ddl%.ddl}
        exampleName=${exampleName#../examples/}
        batsFile="$testName".for-example-"$exampleName".bats
        {
            printf "EXAMPLE=%q\n" "$ddl"
            cat $t
        } >$batsFile
    done
done

# run all .bats tests
bats *.bats
