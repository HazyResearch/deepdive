#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

deepdive sql "
    SELECT weight
         , CASE WHEN wm.description LIKE '%-label1-%' THEN '1(C)'
                                                      ELSE '2(T)'
           END      AS label
         , fn.fname AS feature_name
         , fn.fid   AS feature_id
      FROM dd_inference_result_weights_mapping wm
         , feature_names fn
     WHERE wm.description LIKE ('%--' || fn.fid)
"
