#!/usr/bin/env bash
# How to build the sampler submodule
set -euo pipefail
cd "${0%.build.sh}"

make clean-packages
make package

# create a uniform name for easier build caching and staging
rm -f mindbender-LATEST.sh
for pkg in mindbender-LATEST-*.sh; do
    [[ -e "$pkg" ]] || continue
    ln -sfnv "$pkg" mindbender-LATEST.sh
    break
done
