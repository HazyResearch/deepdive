#!/usr/bin/env bats
# Helper functions for spouse example for PostgreSQL

getAccuracyPerCent() {
    deepdive sql eval "
        SELECT 100 * (num_correct::REAL / (num_correct + CASE WHEN num_incorrect IS NULL THEN 0 ELSE num_incorrect END)) :: INT
          FROM has_spouse_is_true_calibration
         WHERE bucket = 9
    "
}
