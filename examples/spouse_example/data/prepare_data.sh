#!/usr/bin/env bash
# A script for preparing data for DeepDive's spouse example
set -eu
cd "$(dirname "$0")"

bunzip2 --verbose --keep --force {spouses,non-spouses}.*.bz2 || true

# TODO remove this after migrating integration tests out of Scala
bunzip2 --verbose --keep         sentences_dump*.bz2 || true
