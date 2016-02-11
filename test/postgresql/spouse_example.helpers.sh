#!/usr/bin/env bats
# Helper functions for spouse example for PostgreSQL

getAccuracyPerCent() {
    local variable=${1:-has_spouse_label}
    deepdive sql eval "
        SELECT 100 * (num_correct::REAL / (num_correct + num_incorrect)) :: INT
          FROM ${variable}_calibration
         WHERE bucket = 9
    " |
    tee >(echo >&2 "Accuracy for $variable with p >= 0.9 is: $(cat)")
}
