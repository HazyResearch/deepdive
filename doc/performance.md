---
layout: default
---

# Performance Tuning

If you are dealing with large amounts of data then you may find bottlenecks in various parts of the system. The following describes parameters you can use to improve performance.

### Setting the JVM heap size

When running DeepDive you should set the maximum heap size for the Java virtual machine using the "-Xmx" flag. The default heap size is the smaller of 1/4th of the physical memory or 1GB. When using [SBT](http://www.scala-sbt.org/) to run DeepDive, you can set the option as follows:

    SBT_OPTS="-Xmx8g" sbt "run -c path_to_application.conf"

### Setting extractor parallelism and batch sizes

You can execute one extractor in parallel on multiple threads using the `paralleism` option. You can use the `input_batch_size` option to define how many tuples each extractor should receive at once, and the `output_batch_size` option to define how many extracted tuples should be inserted into the data store at once. The [extractor documentation](extractors.html) contains more details about these options. 

### Setting the batch size for factor graph construction

By default, DeepDive inserts variables, factors and weights in batches defined by the underlying data store. The default for PostgreSQL is 50,000. If you have a large amount of memory you may overwrite the batch size using the following configuration setting.

    deepdive.inference.batch_size = 100000


### Gibbs sampler options

You can optionally parse java options (such as -Xmx) and command line options to the gibbs sampler executable. The default Java options are `-Xmx4g` and the default sampler options are `-l 1000 -s 10 -i 1000 -t 4`. Allowed options are:

    -l <value> | --learning_epochs <value>
          number of epochs for learning (required)
    -i <value> | --inference_epochs <value>
          number of epochs for inference (required)
    -s <value> | --learning_samples_per_epoch <value>
          number of samples for learning per epoch

You an specify options in DeepDive as follows:

    deepdive.sampler.java_args: "-Xms1g -Xmx8g"
    deepdive.sampler.sampler_args: "-l 1000 -s 10 -i 1000"