#!/usr/bin/env bash
# deepdive-do-getopts.sh -- Processes command-line flags common for deepdive-do commands
# > . deepdive-do-getopts.sh
##

# non-interactive mode is default
: ${DEEPDIVE_INTERACTIVE:=false}
# but when interactive, allow to review/edit the plan
: ${DEEPDIVE_PLAN_EDIT:=true} ${VISUAL:=${EDITOR:=vi}}
: ${DEEPDIVE_COMPILE_ASK:=true}

# process some simple command-line flags
while getopts "im" o; do
    case $o in
        i) # interactive mode
            export DEEPDIVE_INTERACTIVE=true
            ;;
        m) # manual compilation: disable automatic deepdive-compile
            export DEEPDIVE_AUTOCOMPILE=false
            ;;
    esac
done
shift $(($OPTIND - 1))

# sanity checks and adjustments for dependent flags
if $DEEPDIVE_INTERACTIVE; then
    # check tty
    [[ -t 0 && -t 1 ]] ||
        error "DEEPDIVE_INTERACTIVE=true (-i option) requires a terminal (tty)"

    # ask to compile if needed
    if $DEEPDIVE_COMPILE_ASK && ! app-has-nothing-to-compile; then
        read -p 'Run `deepdive compile` now? ([Y*]es or [n]o): ' -n1 || true; echo
        case $REPLY in
            [nN])
                export DEEPDIVE_AUTOCOMPILE=false
                ;;
            *)
                deepdive-compile
        esac
        export DEEPDIVE_COMPILE_ASK=false
    fi
else
    # disable plan editing
    export DEEPDIVE_PLAN_EDIT=false
fi
