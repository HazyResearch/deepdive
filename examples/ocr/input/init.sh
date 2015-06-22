#!/usr/bin/env bash
# A script to initialize database for DeepDive's OCR example
set -eu
cd "$(dirname "$0")"/raw

python gen_feature_table.py

deepdive sql update <./feature_table.csv "
    COPY features(word_id, feature_id, feature_val)
    FROM STDIN DELIMITER ',' CSV;
"
deepdive sql update <./label1_table.csv "
    COPY label1(wid, val)
    FROM STDIN DELIMITER ',' CSV;
"
deepdive sql update <./label2_table.csv "
    COPY label2(wid, val)
    FROM STDIN DELIMITER ',' CSV;
"
