#!/usr/bin/env bash
# End-to-end test for DeepDive incremental support
set -eu
cd "$(dirname "$0")"

# database name to use (prefix)
: ${DBNAME:=deepdive_test_${USER}}
DBPREFIX=$DBNAME

# subsample sentences for quicker test
: ${INCREMENTAL_SPOUSE_EXAMPLE_LIMIT_SENTENCES:=2000}
export INCREMENTAL_SPOUSE_EXAMPLE_LIMIT_SENTENCES
# require cosine similarity between incremental and non-incremental distributions to be no lower than 0.9
: ${INCREMENTAL_SPOUSE_EXAMPLE_MIN_SIMILARITY:=0.9}

TestUtils=$(cd ../util/test; pwd)
set -x

## Use spouse example
cd ../examples/spouse_example/postgres/incremental

## Incremental runs
export DBNAME=${DBPREFIX}_inc
./0-setup.sh                  spouse_example.f1.ddl        inc.base.out
# Prepare incremental runs with only F1 in base
./1-materialization_phase.sh  spouse_example.f1.ddl        inc.base.out
# Run F2 incrementally
./2-incremental_phase.sh      spouse_example.f2.ddl        inc.base.out  inc.f1+f2.out
./4-merge.sh                  spouse_example.f1.ddl        inc.base.out
# Run Symmetry incrementally after merging
./2-incremental_phase.sh      spouse_example.symmetry.ddl  inc.base.out  inc.f1+f2+symmetry.out


## Non-incremental runs
export DBNAME=${DBPREFIX}_noninc
# Run as usual for F1+F2
./0-setup.sh               spouse_example.f1+f2.ddl             noninc.f1+f2.out
./9-nonincremental_run.sh  spouse_example.f1+f2.ddl             noninc.f1+f2.out
# Run as usual for F1+F2+Symmetry
./0-setup.sh               spouse_example.f1+f2+symmetry.ddl    noninc.f1+f2+symmetry.out
./9-nonincremental_run.sh  spouse_example.f1+f2+symmetry.ddl    noninc.f1+f2+symmetry.out



## Compare similarity between incremental and non-incremental runs
set +x
# using test utilities
PATH="$TestUtils:$PATH"
compare() {
    local what=$1
    local cos_sim=$(cosine_similarity_of <(
                probability_distribution_of dd_new_has_spouse    inc.${what}.out
            ) <(
                probability_distribution_of        has_spouse noninc.${what}.out
            ))
    if [[ $(bc <<<"$cos_sim >= $INCREMENTAL_SPOUSE_EXAMPLE_MIN_SIMILARITY") == 1 ]]; then
        echo >&2 "PASS: $what: incremental and non-incremental probability distributions are similar enough (cosine similarity = $cos_sim)"
    else
        echo >&2 "FAIL: $what: incremental and non-incremental probability distributions are NOT similar enough (cosine similarity = $cos_sim)"
        false
    fi
}
compare f1+f2
compare f1+f2+symmetry
