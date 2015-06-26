#!/usr/bin/env bash
# Enumerate all tests runnable on the current system
set -eu
cd "$(dirname "$0")"/..

list_executable_bats() {
     find "$@" -name '*.bats' -perm -0111 -maxdepth 1
} 2>/dev/null

list_executable_bats test
for testDir in test/*/env.sh; do
    testDir=${testDir%/env.sh}
    testShouldWork="$testDir"/should-work.sh
    ! [[ -x "$testShouldWork" ]] || "$testShouldWork" || continue
    list_executable_bats "$testDir"
done
