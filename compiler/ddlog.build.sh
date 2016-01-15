#!/usr/bin/env bash
# How to build the sampler submodule
set -euo pipefail
cd "${0%.build.sh}"

make ddlog.jar
