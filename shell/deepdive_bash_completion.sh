#!/usr/bin/env bash
# bash completion for deepdive command
# Usage: source $(deepdive whereis etc/deepdive_bash_completion.sh)

_deepdive() {
    local cur prev first opts
    COMPREPLY=()
    cur=${COMP_WORDS[COMP_CWORD]}
    prev=${COMP_WORDS[COMP_CWORD-1]}
    first=${COMP_WORDS[1]}
    opts=`
        list_deepdive_targets() {
            deepdive plan 2>&1 >/dev/null | sed '1d'
        }
        case $COMP_CWORD in
            1)
                deepdive 2>/dev/null |
                sed '
                    1,/^# Available COMMANDs/d
                    s/^deepdive  *//; s/#.*$//
                '
                ;;
            *)
                case $first in
                    plan|"do"|redo)
                        list_deepdive_targets
                        ;;
                    mark)
                        case $COMP_CWORD in
                            2) deepdive mark 2>/dev/null | sed '
                                    /^deepdive mark/!d
                                    s/^deepdive mark  *//; s/  *.*$//'
                                ;;
                            *)
                                list_deepdive_targets
                        esac
                        ;;
                esac
        esac
    `
    COMPREPLY=($(compgen -W "${opts}" -- ${cur}))
}
complete -F _deepdive deepdive
