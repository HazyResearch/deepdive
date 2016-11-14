#!/usr/bin/env bats
# Tests for incremental support

# subsample sentences for quicker test
: ${INCREMENTAL_SPOUSE_EXAMPLE_LIMIT_SENTENCES:=2000}
export INCREMENTAL_SPOUSE_EXAMPLE_LIMIT_SENTENCES
# require cosine similarity between incremental and non-incremental distributions to be no lower than 0.9
: ${INCREMENTAL_SPOUSE_EXAMPLE_MIN_SIMILARITY:=0.9}

# database name to use (prefix)
: ${DBNAME:=deepdive_test_${USER}}
DBPREFIX=$DBNAME  # TODO use DEEPDIVE_DB_URL

load test_environ

setup() {
    "$DEEPDIVE_SOURCE_ROOT"/examples/spouse_example/data/prepare_data.sh
    cd "$DEEPDIVE_SOURCE_ROOT"/examples/spouse_example/postgres/incremental
}

@test "incremental spouse example (F1, then F2 and Symmetry incrementally)" {
    export DBNAME=${DBPREFIX}_inc
    export DEEPDIVE_DB_URL=${DEEPDIVE_DB_URL}_inc
    rm -rf inc.base.out inc.f1+f2.out inc.f1+f2+symmetry.out
    ./0-setup.sh                  spouse_example.f1.ddlog        inc.base.out
    # Prepare incremental runs with only F1 in base
    ./1-materialization_phase.sh  spouse_example.f1.ddlog        inc.base.out
    # Run F2 incrementally
    ./2-incremental_phase.sh      spouse_example.f2.ddlog        inc.base.out  inc.f1+f2.out
    # Merge F2 into F1
    ./4-merge.sh                  spouse_example.f1.ddlog        inc.base.out
    # Run Symmetry incrementally after merging
    ./2-incremental_phase.sh      spouse_example.symmetry.ddlog  inc.base.out  inc.f1+f2+symmetry.out
}

@test "incremental spouse example's non-incremental run (F1+F2)" {
    export DBNAME=${DBPREFIX}_noninc
    export DEEPDIVE_DB_URL=${DEEPDIVE_DB_URL}_noninc
    rm -rf noninc.f1+f2.out
    # Run as usual for F1+F2
    ./0-setup.sh               spouse_example.f1+f2.ddlog             noninc.f1+f2.out
    ./9-nonincremental_run.sh  spouse_example.f1+f2.ddlog             noninc.f1+f2.out
}

@test "incremental spouse example's non-incremental run (F1+F2+Symmetry)" {
    export DBNAME=${DBPREFIX}_noninc
    export DEEPDIVE_DB_URL=${DEEPDIVE_DB_URL}_noninc
    rm -rf noninc.f1+f2+symmetry.out
    # Run as usual for F1+F2+Symmetry
    ./0-setup.sh               spouse_example.f1+f2+symmetry.ddlog    noninc.f1+f2+symmetry.out
    ./9-nonincremental_run.sh  spouse_example.f1+f2+symmetry.ddlog    noninc.f1+f2+symmetry.out
}

compare() {
    local PATH="$DEEPDIVE_SOURCE_ROOT/util/build/test:$PATH"
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
@test "similarity of probability distributions of incremental and non-incremental spouse example (F1+F2)" {
    case $(uname) in Linux) ! [[ -e /proc/self/numa_maps ]] || skip; esac # FIXME skip until sampler inc mode is fixed under NUMA
    compare f1+f2
}
@test "similarity of probability distributions of incremental and non-incremental spouse example (F1+F2+Symmetry)" {
    case $(uname) in Linux) ! [[ -e /proc/self/numa_maps ]] || skip; esac # FIXME skip until sampler inc mode is fixed under NUMA
    compare f1+f2+symmetry
}
