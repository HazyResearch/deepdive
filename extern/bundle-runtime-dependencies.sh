#!/usr/bin/env bash
# A script for building runtime dependencies of DeepDive
##
set -eu

# ensure buildkit submodule is there
here=$(dirname "$0")
Submodule="${here#$PWD/}"/buildkit
[[ -e "$here"/buildkit/.git ]] || git submodule update --init "$Submodule"
! grep -qv '^ ' <(git submodule status "$Submodule") || {
    echo "# ERROR: submodule $Submodule has different commit checked out, retry after running either:"
    echo "  git submodule update $Submodule  # to drop changes and checkout the recorded one, or:"
    echo "  git add $Submodule               # to use the currently checked out one"
    false
} >&2
cd "$here"

# prepare runtime dependencies to bundle
[[ -d .build ]] || {
    mkdir -p ../.build/extern
    ln -sfnv ../.build/extern .build
}
PATH="$PWD"/buildkit:"$PATH"
buildkit/depends/module.build

# drop some unused, large binaries
rm -f .build/bundled/{.all,graphviz/prefix}/bin/{cluster,edgepaint,gvmap*}
