#!/usr/bin/env bash
# A script for building runtime dependencies of DeepDive
##
set -eu
cd "$(dirname "$0")"

mkdir -p .build
[[ -d .build/buildkit ]] ||
    git clone https://github.com/netj/buildkit.git .build/buildkit
PATH="$PWD"/.build/buildkit:"$PATH"
.build/buildkit/depends/module.build
