#!/usr/bin/env bash
# deepdive's wrapper script to mindbender with its bundled dependencies
##
exec "$(dirname "$0")"/deepdive env \
    sh -c 'exec $(deepdive-whereis installed mindbender/bin/mindbender) "$@"' -- "$@"
