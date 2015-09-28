---
layout: default
title: Performance Tuning
---

# Performance Tuning

<div class="alert alert-danger">(This page is mostly outdated and only accurate up to release 0.6.x.)</div> <!-- TODO rewrite -->

Processing large amounts of data may expose bottlenecks in various parts of the
system. The following sections show how to tune different parameters to obtain
better performances.

### Setting extractor parallelism and batch sizes

For `json_extracor` and `tsv_extractor`, you can execute multiple copies of an extractor in parallel using the
`paralleism` option. In `plpy_extractor` the parallelism is managed by database.

For `json_extracor` and `tsv_extractor`, you can use the `input_batch_size` option to define how
many tuples each extractor should receive at once, and the `output_batch_size`
option to define how many extracted tuples should be inserted into the data
store at once. The [extractor
documentation](../basics/extractors.html#jsonparallelism) contains more details about
these options.

### Setting the batch size for factor graph construction

By default, DeepDive inserts variables, factors and weights in batches defined
by the underlying data store. This can be change by defining a value for the
`inference.batch_size` directive. Refer to the ["Configuration
reference"](../basics/configuration.html#batch_size) for more details about this
directive.

