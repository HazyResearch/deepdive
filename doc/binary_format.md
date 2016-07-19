# DimmWitted Factor Graph in Binary Format

This documents the format of input data for DimmWitted Gibbs Sampler.

DimmWitted uses a custom binary encoding that represents the input factor graph and its weights.
There are five input files in different format: metadata, weights, variables, domains for categorical variables, and factors.
Multiple bytes value must be in [*network byte order* or *Big-endian*](https://en.wikipedia.org/wiki/Endianness).

## Metadata Text
This input is a single line in [CSV (comma-separated values)](https://en.wikipedia.org/wiki/Comma-separated_values) text format that describes the size of the factor graph with the following fields.

    numWeights,numVariables,numFactors,numEdges


## Weights Binary
This input enumerates all weights that can appear in the factor graph.
The following fields must appear in order for each weight.

    weightId        uint64_t    8   // unique id for this weight, used by factors
    isFixed         uint8_t     1   // indicates whether this weight is:
                                    //   fixed (0x01) or to be learned (0x00)
    initialValue    double      8   // value to use when fixed, or
                                    //   initial value to start with when learning

## Variables Binary
This input enumerates all variables present in the factor graph where the following fields are repeated for each variable.

    variableId      uint64_t    8   // unique id for this variable, used by factors
    isEvidence      uint8_t     1   // indicates whether this is:
                                    //   a query variable (0x00),
                                    //   an evidence variable (0x01 and 0x02), or
                                    //   an observation variable (0x02)
    initialValue    uint32_t    4   // initial value to assign
    dataType        uint16_t    2   // indicates whether this is:
                                    //   a Boolean variable (0x0000), or
                                    //   a categorical variable (0x0001)
    cardinality     uint32_t    4   // cardinality of the variable:
                                    //   2 for Boolean variables, or
                                    //   a number for categorical variables

## Categorical Variable Domains Binary
This input enumerates the domain of categorical variables, i.e., the values each variable can take.
The following fields are repeated for each variable.

    variableId      uint64_t    8   // the id of the variable this block describes
    cardinality     uint32_t    4   // cardinality of the variable, which tells how many categoryValues follow
    categoryValue   uint32_t    4   // a value (id) this variable can take
    ...                             // categoryValue must appear exactly cardinality times



## Factors Binary
This input enumerates all factors in the factor graph that refers to variables and weights by their id.
The following fields are repeated for each factor.

    factorFunction  uint16_t    2   // type of factor function, see: FACTOR_FUNCTION_TYPE
    arity           uint32_t    4   // arity of the factor, i.e., how many variables it connects to
    variableReferences              // references to variables (one block per arity)
    weightReferences                // reference to multiple weights (categorical factor) or a single weight id

For valid values for `factorFunction`, see [`FACTOR_FUNCTION_TYPE` enum](../src/common.h).

### Variable References of a Factor
Each block of `variableReferences` consists of the following fields:

    variableId      uint64_t    8   // the variable id for this factor
    equalPredicate  uint32_t    4   // value to check equality against the variable

### Weight References of a Factor
Each block of `weightReferences` must have the following structure when `factorFunction` is `FUNC_AND_CATEGORICAL`:

    numWeights      uint64_t    8   // the number of weights defined for this categorical factor
    categoryValue_i uint32_t    4   // the value id to compare against for a connected variable
    ...                             // categoryValue must appear exactly arity times
    weightId        uint64_t    8   // the weight id for this factor when each connected variable
                                    //   takes the categorical value
    featureValue    double      8   // feature value for this factor

Otherwise, for other `factorFunction`s, the `weightReferences` block is simply:

    weightId        uint64_t    8   // the weight id for this factor
    featureValue    double      8   // feature value for this factor

