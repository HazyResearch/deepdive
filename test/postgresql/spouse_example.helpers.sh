#!/usr/bin/env bats
# Helper functions for spouse example for PostgreSQL

getAccuracyPerCent() {
    local variable=${1:-has_spouse_is_true}
    deepdive sql eval "
        SELECT 100 * (num_correct::REAL / (num_correct + CASE WHEN num_incorrect IS NULL THEN 0 ELSE num_incorrect END)) :: INT
          FROM ${variable}_calibration
         WHERE bucket = 9
    "
}
