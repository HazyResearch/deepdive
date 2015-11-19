#!/usr/bin/env bash
# How to build the sampler submodule
set -euo pipefail
cd "${0%.build.sh}"

[ -e ./lib/gtest -a -e ./lib/tclap ] || make dep
make dw
