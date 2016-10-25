#!/usr/bin/env bash
case $(uname) in
    Darwin)
        # C++11 finagling for Mac OSX
        export CC=clang
        export CXX=clang++
        export MACOSX_VERSION_MIN="10.9"
        CXXFLAGS="$CXXFLAGS -mmacosx-version-min=${MACOSX_VERSION_MIN}"
        CXXFLAGS="$CXXFLAGS -Wno-error=unused-command-line-argument"
        export LDFLAGS="$LDFLAGS -mmacosx-version-min=${MACOSX_VERSION_MIN}"
        export LINKFLAGS="$LDFLAGS"
        export MACOSX_DEPLOYMENT_TARGET=10.9

        # make sure clang-format is installed
        # e.g. brew install clang-format

        # make sure autoreconf can be found
        # e.g. brew install autoconf

        # for graphviz, also brew install autogen libtool
        ;;
esac

# we'll rely on conda for all runtime dependencies
make no-bundled-runtime-dependencies

# build and install under a deepdive subdir in conda prefix
make install PREFIX="$PREFIX/$PKG_NAME"

# expose executables in bin/
for cmd in "$PREFIX/$PKG_NAME"/bin/*; do
    [[ -x "$cmd" ]] || continue
    ln -sfnr "$cmd" "$PREFIX"/bin/
done


## add the /util directory to the PATH inside this conda env
## http://conda.pydata.org/docs/using/envs.html#saved-environment-variables
#mkdir -p "$PREFIX"/etc/conda/activate.d
#echo >"$PREFIX"/etc/conda/activate.d/"$PKG_NAME"-env-activate.sh '#!/usr/bin/env bash
#
#export PRE_'"$PKG_NAME"'_PATH="$PATH"
#export PATH="$CONDA_PREFIX/util:$PATH"
#'
#
#mkdir -p "$PREFIX"/etc/conda/deactivate.d
#echo >"$PREFIX"/etc/conda/deactivate.d/"$PKG_NAME"-env-deactivate.sh '#!/usr/bin/env bash
#
#export PATH="$PRE_'"$PKG_NAME"'_PATH"
#unset PRE_'"$PKG_NAME"'_PATH
#'
