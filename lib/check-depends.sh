#!/usr/bin/env bash
set -eu

# a list of commands DeepDive depends on
needs_command() {
    local cmd=$1; shift
    type "$cmd" >/dev/null
    # TODO check version requirements
}

# runtime dependency
needs_command bash
#needs_command psql
needs_command java
needs_command python
needs_command bc
needs_command gnuplot

needs_command git

# build dependency
needs_command unzip
needs_command javac
needs_command make
#needs_command g++
#needs_command pip
#needs_command gem
