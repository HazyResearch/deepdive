#!/usr/bin/env bash
# A simple script for checking coding style
set -eu
shopt -s extglob

### prepare to check and output violations ####################################
export PAGER=
# use colored output (red, green) for tty
if [[ -t 1 ]]; then
    color() {
        local color=$1; shift
        case $color in
            red) echo -ne $'\E[0;31m' ;;  # use red
            green) echo -ne $'\E[0;32m' ;;  # use green
        esac
        "$@"
        echo -ne $'\E[0m'     # and reset color
    }
else
    color() { shift; "$@"; }
fi
CHECKING() {
    LASTCHECK=$1
    echo >&2 "# CHECKING CODING STYLE: $LASTCHECK"
    trap 'c=$?; color red echo >&2 "# CODING STYLE VIOLATIONS: $LASTCHECK"; exit $c' ERR
}
violations=$(mktemp ${TMPDIR:-/tmp}/checkstyle.XXXXXX)
NO() {
    "$@" >"$violations" || true
    if [[ -s "$violations" ]]; then
        color red cat "$violations"
        return 1
    fi
}

### check coding style by finding violations ##################################

textfiles=(
$(git ls-files | xargs file --mime-type | grep text/ | cut -d: -f1)
)

#CHECKING "All text files should have EOL (end of line) at the end"
#NO find "${textfiles[@]}" -type f \
#    -exec sh -c '[ `tail -1 "$1" | wc -l` -eq 0 ]' -- {} \; -print
#
#CHECKING "All text files should have Unix line endings"
#NO git grep -l -I $'\r''$'
#
#CHECKING "No text file should have trailing spaces"
#NO grep -l '[[:space:]]\+$' -- "${textfiles[@]}"

CHECKING "All code should use spaces instead of tab character"
NO find "${textfiles[@]}" -type f \
    \
    ! -name '.gitmodules' \
    ! -name 'Makefile' ! -name '*.mk' \
    ! -path compiler/compile-code/compile-code-Makefile \
    \
    -exec grep -l '^[[:space:]]*'$'\t' {} +

###############################################################################
color green echo >&2 "# NO CODING STYLE VIOLATIONS! :)"
