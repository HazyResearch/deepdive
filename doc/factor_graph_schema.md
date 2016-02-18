---
layout: default
title: Factor Graph Grounding Output Reference
---

# Factor graph grounding output schema reference

[Grounding](overview.md#grounding) is the process of building the
factor graph and dumping it to files that the [sampler](sampler.md)
can take as input. DeepDive uses a custom binary format to encode the factor
graph. It generates four files, one each for weights, variables, factors and metadata.
All files resides in the `out/` directory of the latest run. The format
of these files is as follows, where numbers are in bytes and **network byte
order** is used.

Weights

    weightId        long    8
    isFixed         bool    1
    initialValue    double  8


Variables

    variableId      long    8
    isEvidence      bool    1
    initialValue    double  8
    dataType        short   2
    edgeCount*      long    8
    cardinality     long    8

Factors

    weightId        long    8
    factorFunction  short   2
    equalPredicate  long    8
    edgeCount       long    8
    variableId1     long    8
    isPositive1     bool    1
    variableId2     long    8
    isPositive2     bool    1
    ...


*Note*: from [version 0.03](changelog/0.03-alpha.md), the edgeCount field
in Variables is always -1.

The systems also generates a metadata file which contains a single line in
comma-separated-values format with the following fields:

    Number of weights
    Number of variables
    Number of factors
    Number of edges
    Path to weights file
    Path to variables file
    Path to factors file
    Path to edges file

### Incremental grounding schema reference

For incremental grouding, the weights, variables, metadata files have the same format as above.
There are two more files, factors and edges files, with the following format

Factors

    factorId        long    8
    weightId        long    8
    factorFunction  short   2
    edgeCount       long    8

Edges

    variableId      long    8
    factorId        long    8
    position        long    8
    isPositive      bool    1
    equalPredicate  long    8

