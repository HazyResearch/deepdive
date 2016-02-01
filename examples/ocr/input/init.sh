#!/usr/bin/env bash
# A script to initialize database for DeepDive's OCR example
set -eu
cd "$(dirname "$0")"/raw

python gen_feature_table.py
