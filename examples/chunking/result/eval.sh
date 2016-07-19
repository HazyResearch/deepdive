#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"

deepdive sql "COPY (
    SELECT c.word, c.pos, c.true_tag, a.tag
      FROM chunk_inference a
      LEFT JOIN chunk_inference b
        ON a.word_id = b.word_id
       AND a.expectation < b.expectation
      LEFT JOIN words c
        ON a.word_id = c.word_id
     WHERE b.expectation IS NULL
     ORDER BY a.word_id
  ) TO STDOUT DELIMITER ' ';" > output

./conlleval.pl <output
