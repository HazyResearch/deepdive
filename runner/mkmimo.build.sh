#!/usr/bin/env bash
# How to build the mkmimo submodule
set -euo pipefail
cd "${0%.build.sh}"

# XXX a temporary workaround until mkmimo's crashing issue for Macs
# running OS X 10.11 El Capitan is completely solved
case $(uname) in
    Darwin) export MKMIMO_IMPL=bash
esac

make
