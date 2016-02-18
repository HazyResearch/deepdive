#!/bin/sh
# jq -- a shebang interpreter for using jq executable files in a portable way
set -e
#jq=`type -p -a jq | grep -vxF "$0" | head -1` # XXX this is slow
jq="$DEEPDIVE_HOME"/lib/bundled/.all/bin/jq  # XXX hardcoded to minimize overhead
if test -n "$1" -a -f "$1" -a -x "$1"; then
    f=$1; shift
    d=`dirname "$f"`
    exec "$jq" -f "$f" -L "$d" "$@"
else
    exec "$jq" "$@"
fi
