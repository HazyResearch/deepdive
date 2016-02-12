#!/usr/bin/env bash
# How to build the Mindbender submodule
set -euo pipefail
cd "${0%.build.sh}"

make polish
