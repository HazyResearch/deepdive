#!/usr/bin/env bash
# A script for building runtime dependencies of DeepDive
##
set -eu

# ensure buildkit submodule is there
here=$(dirname "$0")
[[ -e "$here"/buildkit/.git ]] || git submodule update --init "${here#$PWD/}"/buildkit
cd "$here"

# prepare runtime dependencies to bundle
mkdir -p .build
PATH="$PWD"/buildkit:"$PATH"
buildkit/depends/module.build

# drop some unused, large binaries
rm -f .build/bundled/{.all,graphviz/prefix}/bin/{cluster,edgepaint,gvmap*}
