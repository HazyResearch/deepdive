#!/usr/bin/env bash
# A script for building runtime dependencies of DeepDive
##
set -eu
cd "$(dirname "$0")"

mkdir -p .build
[[ -e buildkit/.git ]] || git submodule update --init buildkit
PATH="$PWD"/buildkit:"$PATH"
buildkit/depends/module.build
