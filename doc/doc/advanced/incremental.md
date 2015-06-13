---
layout: default
---

# Building an Incremental Application

This document describes how to build an incremental application using [DDlog][] and
DeepDive. The example application is [the spouse example](../basics/walkthrough/walkthrough.html).

This document assumes you are familiar with basic concepts in DeepDive and the
spouse application tutorial.

## Incremental application workflow

In building knowledge base construction, a typical scenario is the user has
already built a knowledge base application, and wants to try new features or add
new data. We call the base case *materialization phase*, and the iterative process
of adding features or data *incremental phase*. The user will run materialization
phase to  materialize the base factor graph, and can run multiple incremental
phase on top of the base factor  graph. Each incremental run is independent and
is relative to the base run. Once the user is satisfied with the incremental
experiments, the user can run  *merge phase* to merge the incremental part into
the base part. The workflow can be summarized as follows.

1. Preprocess and load data.
2. Write the DDlog program.
3. Run *materialization phase* to materialize the base factor graph.
4. Add features or add new data.
5. Run *incremental phase*.
6. Repeat 4 and 5 until satisfied.
7. Run *merge phase* to merge the incremental part into the base part.


### Preprocessing and loading data

The incremental version of the spouse example is under [`examples/spouse_example/incremental`](https://github.com/HazyResearch/deepdive/tree/master/examples/spouse_example/incremental).
The only difference is that all the arrays are transformed into string using array_to_string with delimiter '~^~' due to DDlog's limited support for array type.
You can follow the [corresponding section in the original walkthrough](../basics/walkthrough/walkthrough.html#loading_data) to load the data.

Alternatively, you can try the handy scripts included incremental example we include in the source tree.

```bash
cd examples/spouse_example/incremental
./0-setup.sh spouse_example.f1.ddl inc-base.out
```


### Writing Application in DDlog

In order to make use of the incremental support of DeepDive, the application must be written in DDlog.
Please refer to [DDlog tutorial][DDlog] for how to write your DeepDive application in DDlog.
Let's assume you have put the DDlog program shown below in a file named `spouse_example.f1.ddl` under the application folder.

<script src="http://gist-it.appspot.com/https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/incremental/spouse_example.f1.ddl?footer=minimal">
</script>

In this walkthrough, we demonstrate an incremental development workflow by adding new features.
In the udf `ext_has_spouse_features.f1.py`, suppose you had only feature 1 at the time of materialization phase.
Then, suppose you want to try feature 2 as an incremental phase.

Note that the [`run.sh` included in the incremental example](https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/incremental/run.sh) takes care of compiling a given DDlog program into a particular mode.


### Materialization Phase

Materialization phase is basically for taking a snapshot of the current model that serves as a base for the following incremental phases.
You need to specify which variables and inference rules you are going to vary in the following incremental phases.
They are called active variables and active inference rules, and the names can be put into files `spouse_example.f1.active.vars` and `spouse_example.f1.active.rules`.

<script src="http://gist-it.appspot.com/https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/incremental/spouse_example.f1.active.vars?footer=minimal">
</script>

<script src="http://gist-it.appspot.com/https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/incremental/spouse_example.f1.active.rules?footer=minimal">
</script>

There is a script included in the example, and the materialization phase can be run using the following single command:
```bash
./1-materialization_phase.sh spouse_example.f1.ddl inc-base.out
```

It will do the following:
1. The DDlog program `spouse_example.f1.ddl` is compiled.
2. The `extraction` and `inference` pipelines are run.
3. A materialized base factor graph is produced under `./inc-base.out/` under the application directory.


### Incremental Phase

In the incremental phase, suppose you added feature 2 to your udf, `ext_has_spouse_features.f2.py`.
Let's say you saved this modified udf in a file named `ext_has_spouse_features.f2.ddl`.

<script src="http://gist-it.appspot.com/https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/incremental/udf/ext_has_spouse_features.f2.py?footer=minimal&slice=27:39">
</script>


You need to mark in the DDlog program which udf has been modified to produce the correct incremental pipeline.
Let's say you saved this modified DDlog program in a file named `spouse_example.f2.ddl`.

<script src="http://gist-it.appspot.com/https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/incremental/spouse_example.f2.ddl?footer=minimal&slice=67:70">
</script>


Finally, there is also a script included in the example, so the incremental phase can be done as simple as running the following command:

```bash
./2-incremental_phase.sh spouse_example.f2.ddl ./inc-base.out/ ./inc-f1+f2.out/
```

The incremental result computed from `./inc-base.out/` will be stored in `./inc-f1+f2.out/`.


#### Repeating Incremental Phase

The incremental phase can be repeated several times until you are satisfied with the result.
You need to run the following command before running another incremental phase:

```bash
./3-cleanup.sh spouse_example.f2.ddl ./inc-f1+f2.out/
```


### Merge Phase

Once you decide to keep the current version, you can merge the incremental part into the base with the `--merge` mode:

```bash
./4-merge.sh spouse_example.f2.ddl ./inc-f1+f2.out/
```

Finally, you've finished a full workflow of the incremental application development cycle.


## Non-incremental Runs

In contrast, you can see how the same application written in DDlog can be run from scratch, non-incrementally with the following series of commands:

```bash
export DBNAME=deepdive_spouse_noninc
./0-setup.sh              spouse_example.f1+f2.ddl ./noninc-f1+f2.out/
./9-nonincremental_run.sh spouse_example.f1+f2.ddl ./noninc-f1+f2.out/
```


[DDlog]: ../basics/ddlog.html
