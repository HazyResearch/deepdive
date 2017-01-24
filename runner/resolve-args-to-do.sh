#!/usr/bin/env bash
# resolve-run-targets.sh -- Resolves unqualified names to do into run/Makefile targets
##

: ${DEEPDIVE_APP_CHECKED_COMPILED:=false}

DEEPDIVE_APP=$(find-deepdive-app)
export DEEPDIVE_APP

# check if already fully compiled first
if $DEEPDIVE_APP_CHECKED_COMPILED || app-has-been-compiled; then
    # mark in the environment so redundant checks in subprocesses can be skipped
    export DEEPDIVE_APP_CHECKED_COMPILED=true
else
    false
fi

# when our make commands are nested under an outer make, e.g., our own `make
# test`, some special environment variables can screw things up, so clear them
unset MAKEFLAGS MFLAGS MAKEOVERRIDES MAKELEVEL

# find what exactly each argument refers to
if [[ $# -gt 0 ]]; then
    makeTargets=()
    for target; do
        # TODO recognize all
        resolved=false
        for fmt in %s %s.done {data,process,data/model,model,process/{model,grounding{,/{factor,variable}}}}/%s.done; do
            makeTarget=$(printf "$fmt" "$target")
            if [[ $(make -C "$DEEPDIVE_APP"/run -p | sed 's/:.*$//' | grep -cxF "$makeTarget") -gt 0 ]]; then
                makeTargets+=("$makeTarget")
                resolved=true
                break
            fi
        done
        $resolved || error "$target: Unknown target"
    done
    set -- "${makeTargets[@]}"
else
    usage "$0" "No TARGET specified, which can be one of:" ||
    make -C "$DEEPDIVE_APP"/run --silent list >&2
    false
fi
