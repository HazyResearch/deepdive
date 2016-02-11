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
            deepdive plan 2>&1 >/dev/null | sed -n '
                # (skip first line as it is not a target name)
                1d
                # the fully qualifed target name
                p; h
                # and extra shorthands without the prefixes recognized by runner/resolve-args-to-do.sh
                g; s:^data/::p
                g; s:^process/::p
                g; s:^data/model/::p
                g; s:^model/::p
                g; s:^process/model/::p
                g; s:^process/grounding/::p
                g; s:^process/grounding/factor/::p
                g; s:^process/grounding/variable/::p
            '
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
                    plan|"do"|redo|run)
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
                    model)
                        case $COMP_CWORD in
                            2) deepdive model 2>/dev/null | sed '
                                    /^deepdive model/!d
                                    s/^deepdive model  *//; s/  *.*$//'
                                ;;
                            3)
                                case ${COMP_WORDS[2]} in
                                    factorgraph|weights)
                                        echo init
                                        echo list
                                        echo keep
                                        echo reuse
                                        echo drop
                                esac
                                ;;
                            *)
                                case ${COMP_WORDS[2]} in
                                    factorgraph|weights)
                                        deepdive model ${COMP_WORDS[2]} list
                                esac
                        esac
                        ;;
                esac
        esac
    `
    COMPREPLY=($(compgen -W "${opts}" -- ${cur}))
}
complete -F _deepdive deepdive
