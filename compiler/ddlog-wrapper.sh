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

# if nailgun is available, use it to avoid JVM bootup latency
if type ng-nailgun &>/dev/null; then
    ng-server-is-running() { (exec -a ng-version ng-nailgun &>/dev/null); }
    if ! ng-server-is-running; then
        # launch a JVM with nailgun if one isn't running already
        NAILGUN_JAR=/usr/share/java/nailgun.jar
        java -cp "$Here"/../lib/ddlog.jar:"$NAILGUN_JAR" \
            -server com.martiansoftware.nailgun.NGServer &>/dev/null &
        until ng-server-is-running; do sleep 0.1; done
    fi
    # TODO --nailgun-Duser.dir="$PWD" but not pass down to DeepDiveLog
    exec ng-nailgun org.deepdive.ddlog.DeepDiveLog "$@"
fi

# launch jar
exec java -jar "$Here"/../lib/ddlog.jar "$@"
