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
rm -vf .build/bundled/{.all,graphviz/prefix}/bin/{cluster,edgepaint,gvmap*}

# put aside postgresql executables from .all/bin/ so they don't mask user's choice
# TODO this should be handled by restoring user env upon execution
( cd .build/bundled
mkdir -p .all/postgresql
for exe in postgresql/prefix/bin/*; do
    [[ -x "$exe" ]] || continue
    exe=$(basename "$exe")
    [[ -x .all/bin/"$exe" ]] || continue
    mv -vf .all/bin/"$exe" .all/postgresql/
done
)

# mark as done
touch .build/bundled
