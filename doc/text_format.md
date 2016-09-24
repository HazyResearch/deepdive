# DimmWitted Factor Graph in Textual Format

DimmWitted provides a handy way to generate the custom binary format from text (TSV; tab-separated values).
`dw text2bin` and `dw bin2text` speaks the textual format described below.

TODO polish the following

## Weights TSV
* `weights*.tsv`
    1. wid, e.g., `100`
    2. is fixed (`1`) or not (`0`)
    3. weight value, e.g., `0`, `2.5`


## Variables TSV
* `variables*.tsv`
    1. vid, e.g., `0`
    2. is evidence (`0` or `1`)
    3. initial value
    4. variable type (`0` for Boolean, `1` for categorical)
    5. cardinality

## Domains TSV
* `domains*.tsv`
    1. vid, e.g., `0`
    2. cardinality, e.g., `3`.
    3. array of domain values, e.g., `{2,4,8}`.


## Factors TSV
* `factors*.text2bin-args`
    1. factor function id (See: [`enum FACTOR_FUNCTION_TYPE`](https://github.com/HazyResearch/sampler/blob/master/src/common.h))
    2. arity: how many variables are connected, e.g., `1` for unary and `2` for binary
    3. a variable value to compare against the variable in corresponding position (one per arity), e.g., negative (`0`) and positive (`1`) for Boolean variables

* `factors*.tsv`
    1. vids: one or more depending on the given arity delimited by tabs, e.g., `0	1` for binary factors
    1b. value ids (present only for FUNC_AND_CATEGORICAL), e.g., `3  6`
    2. wid, e.g., `100`
    3. feature value, e.g., `3`
