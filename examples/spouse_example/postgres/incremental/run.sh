#!/usr/bin/env bash
# A lower-level script for compiling DDlog program and running it with DeepDive
# Usage: run.sh DDLOG_FILE DDLOG_MODE PIPELINE [DEEPDIVE_ARG...]
#
# DDLOG_MODE is one of: --materialization or --incremental or --merge
#
# Not intended to be used directly by users.  Please use the higher-level scripts.
set -eux

DDlog=$1; shift
Mode=$1; shift
Pipeline=$1; shift
Out=$1; shift

# sanitize Mode and default to original
case $Mode in
    --materialization|--incremental|--merge) ;;
    *) Mode=
esac

appConf="${DDlog%.ddl}${Mode:+.${Mode#--}}.application.conf"
userConf="$(dirname "$DDlog")"/application.conf

# compile application.conf from DDlog if necessary
[[ "$appConf" -nt "$DDlog" && "$appConf" -nt "$userConf" ]] || {
    "$DEEPDIVE_HOME"/util/ddlog compile $Mode "$DDlog"
    cat "$userConf"
} >"$appConf"

# XXX `readlink -f` isn't portable, hence these nasty workarounds
appConf="$(cd "$(dirname "$appConf")" && pwd)/$(basename "$appConf")"
Out=$(mkdir -p "$Out" && cd "$Out" && pwd)
BASEDIR=${BASEDIR:+$(mkdir -p "$BASEDIR" && cd "$BASEDIR" && pwd)}

# ddlog-generated application.conf contains a PIPELINE, so we must set it here
export PIPELINE=$Pipeline

# run DeepDive, passing the rest of the arguments
# TODO use deepdive run instead
java -cp "$(cat "$DEEPDIVE_HOME"/shell/deepdive-run.classpath)" \
    org.deepdive.Main -c "$appConf" -o "$Out"  "$@"
