#!/usr/bin/env bats
# Helper functions for spouse example for MySQL

getAccuracyPerCent() {
    deepdive sql eval "
        SELECT ROUND(100 * (num_correct / (num_correct + CASE WHEN num_incorrect IS NULL THEN 0 ELSE num_incorrect END)))
          FROM has_spouse_is_true_calibration
         WHERE bucket = 9
    "
}
