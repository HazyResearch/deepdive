#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"

# make sure bats is available
PATH="$PWD/bats/bin:$PATH"
type bats &>/dev/null ||
    git clone https://github.com/sstephenson/bats.git

# run all .bats tests
c=0
for t in *.bats; do
    case $t in
        *-example.bats)
            # run bats test for every example
            for ddl in ../examples/*.ddl; do
                EXAMPLE=$ddl bats $t || c=$?
            done
            ;;

        *)
            # otherwise, simply run the bats
            bats $t
    esac
done
exit $c
