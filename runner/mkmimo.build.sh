#!/usr/bin/env bash
# How to build the mkmimo submodule
set -euo pipefail
cd "${0%.build.sh}"

make
