---
layout: default
---

# Performance Tuning 

Processing large amounts of data may expose bottlenecks in various parts of the
system. The following sections show how to tune different parameters to obtain
better performances.

### <a name="parallelgrounding" href="#"></a> Parallel grounding
[Grounding](../basics/overview.html#grounding) is the process of building the
factor graph. If you are using Greenplum, you can enable parallel grounding to
speed up the grounding phase, which makes use of Greenplum's parallel file
system (gpfdist). To use parallel grounding, first make sure that Greenplum's file
system server `gpfdist` is running locally, i.e., on the machine where you will
run the DeepDive applications. If it is not running, you can use the following
command to start gpfdist

    gpfdist -d [directory] -p [port] &

where you specify the directory for storing the files and the HTTP port to run on.
The directory should be an **empty directory** since DeepDive will clean up
this directory or overwrite files.
Then, in `application.conf`, specify the gpfdist settings in the `db.default` as
follows

    db.default {
      gphost   : [host of gpfdist]
      gpport   : [port of gpfdist]
      gppath   : [**absolute path** of gpfdist directory]
    }

where gphost, gpport, gppath are the host, port, and absolute path 
gpfdist is running on (specified when starting gpfdist server).

Finally, tell DeepDive to use parallel grounding by adding the following to
`application.conf`: 

    inference.parallel_grounding: true
    
### <a name="parallelloading" href="#"></a> Parallel loading / dumping in extractors

When using [TSV extractors](../basics/extractors.html#tsv_extractor), 
the system dumps the input SQL query to disk, run the UDF in parallel to process the data,
and load the data back to database. The default dumping and loading is single-threaded,
which often becomes the bottleneck for extractors in large-scale applications. 

If you are using a Greenplum, you can enable parallel dumping and loading to
speed up the extraction phase. Parallel dumping uses the same mechanism with 
[Parallel grounding](#parallelgrounding) mentioned above, and loading uses 
`gpload`, a parallel loading utility provided by Greenplum.

You should set up `gpfdist` and `gphost`, `gpport`, `gppath` in `db.default` introduced in [Parallel grounding](#parallelgrounding) above. You should also make sure the command-line utility `gpload` is available on the client where DeepDive runs.

Then, tell DeepDive to use parallel loading in extractors by adding the following to
`application.conf`: 

    extraction.parallel_loading: true

If you have wide rows in extractor input queries, you may get 
"line too long" errors, in this case please refer to the 
[FAQ page](../basics/faq.html#gpfdistmaxlen).

### Setting the JVM heap size

Use the `-Xmx` flag to the `java` command to set the maximum heap size for the
Java Virtual Machine. The default heap size is the minimum between one quarter of
the physical memory and 1GB. If you use [SBT](http://www.scala-sbt.org/) to run
a DeepDive application , you can set the heap size as follows:
```bash
    SBT_OPTS="-Xmx8g" sbt "run -c path_to_application.conf"
```

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

