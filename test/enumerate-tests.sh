#!/usr/bin/env bash
# Enumerate all tests runnable on the current system
set -eu
cd "$(dirname "$0")"/..

list_executable_bats() {
     find -L "$@" -name '*.bats' -perm -0111 -maxdepth 1 || true
} 2>/dev/null

{
list_executable_bats test {shell,util,database,compiler,runner,inference,ddlib}/test
for testDir in test/*/test_environ.bash; do
    testDir=${testDir%/test_environ.bash}
    testShouldWork="$testDir"/should-work.sh
    ! [[ -x "$testShouldWork" ]] || "$testShouldWork" || continue
    list_executable_bats "$testDir"
    list_executable_bats "$testDir"/scalatests
done
} | sort
