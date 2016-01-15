#!/usr/bin/env bash
# ddlog.jar wrapper

# find itself
Self=$(readlink -f "$0" 2>/dev/null || {
    # XXX readlink -f is only available in GNU coreutils
    cd $(dirname -- "$0")
    n=$(basename -- "$0")
    if [ -L "$n" ]; then
        L=$(readlink "$n")
        if [ x"$L" != x"${L#/}" ]; then
            echo "$L"; exit
        else
            cd "$(dirname -- "$L")"
            n=$(basename -- "$L")
        fi
    fi
    echo "$(pwd -P)/$n"
})
Here=$(dirname "$Self")

# launch jar
exec java -jar "$Here"/../lib/ddlog.jar "$@"
