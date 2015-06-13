#!/usr/bin/env bash
# A script for preparing data for DeepDive's spouse example
set -eu
cd "$(dirname "$0")"

bunzip2 --verbose --keep data/*.bz2 || true
