Spouse Example
====

This directory contains 3 different implementation of spouse example listed online.

Use these examples to learn about different extractor types.


Performance Comparison
----

Environment: Macbook Retina Pro, PostgreSQL database.

### json_extractor (default)

    --------------------------------------------------
    ext_people SUCCESS [8559 ms]
    ext_has_spouse_candidates SUCCESS [3811 ms]
    ext_has_spouse_features SUCCESS [8401 ms]
    --------------------------------------------------

### plpy_extractor

    --------------------------------------------------
    ext_people SUCCESS [3893 ms]
    ext_has_spouse_candidates SUCCESS [2188 ms]
    ext_has_spouse_features SUCCESS [3371 ms]
    --------------------------------------------------

### tsv_extractor

    --------------------------------------------------
    ext_people SUCCESS [5826 ms]
    ext_has_spouse_candidates SUCCESS [5744 ms]
    ext_has_spouse_features SUCCESS [4744 ms]
    --------------------------------------------------
