#!/usr/bin/env bash
# How to build the sampler submodule
set -euo pipefail
cd "${0%.build.sh}"

case $(uname) in
    Darwin)
        # TODO use clang >= 3.7
        ;;

    *)
        : ${CXX:=g++}  # default to using GCC's g++
        if "$CXX" --version | grep -q 'Free Software Foundation'; then
            # require GCC >= 4.8
            gxx_is_recent_enough() {
                set -x
                local gxx_current_version=$("$CXX" --version | head -1 | sed '
                    s/[[:space:]][0-9]*$//
                    s/.*[[:space:]]//
                    /^[0-9]*\(\.[0-9]*\)*/!d
                    s/\([0-9]*\)\.\([0-9]*\).*/\1.\2/
                ')
                [ -n "$gxx_current_version" -a ! "$gxx_current_version" '<' 4.8 ] || return 1
            }
            gxx_is_recent_enough || {
                # look for a recent version of GCC
                for version in 5.3 5.2 5.1 5.0 4.9 4.8; do
                    if type g++-$version &>/dev/null; then
                        CXX=g++-$version
                    fi
                done
                gxx_is_recent_enough || {
                    echo >&2 "g++ >= 4.8 required to build"
                    false
                }
            }
        fi
        export CXX
esac

lib/has-all-dep.sh || make -j dep
make -j dw || {
    # retry build after clean
    make clean
    make -j dw
}
