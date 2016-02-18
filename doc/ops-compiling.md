---
layout: default
title: Compiling a DeepDive application
---

# Compiling a DeepDive application

The first step to run a DeepDive application is to compile it.
We describe here the use of the different `deepdive` commands, why and where it compiles and which files can be interesting to look at.


## How to compile

To compile a DeepDive application, simply run the following command within the DeepDive app.

```bash
deepdive compile
```

All subsequent `deepdive do` commands will run what has been compiled, so an app has to be compiled at least once initially before [executing any part of it](ops-execution.md).



## What is compiled

All compiled output is created under the `run/` directory of the app:

* `run/dataflow.svg`

    A data flow diagram showing the dependencies between processes.
    This file contains a graph of the dataflow and helps better understand the dependencies among relations, rules and user-defined functions used to produce them.
    It can be opened in a web browser (Chrome or Safari work particularly well for it).

* `run/Makefile`

    A Makefile that mirrors the dependencies, used for generating execution plans for running certain parts of the data flow.

* `run/process/**/run.sh`

    Shell scripts that contain what actually should be run for every process.
    These scripts will be run by certain `deepdive do` commands.

* `run/LATEST.COMPILE/` and `run/compiled/`

    Each compilation step creates a unique timestamped directory under `run/` to store all the intermediate representation used for compilation (`*.json`).
    In particular, the latest compiled version can be found in `run/LATEST.COMPILE/`.
    For instance, the `deepdive.conf` used for the latest compilation can be found at `run/LATEST.COMPILE/deepdive.conf`.

    * `config.json`

        The compiled final configuration object.

    * `code-*.json`

        The compiled object for generating actual code.

    * `compile.log`

        The log file left by the latest compilation step.

If you keep track of your DeepDive application with Git, it may be important to add a `/run` line in `.gitignore`.


## When to compile

The following files in [a DeepDive application](deepdiveapp.md) are considered as the input to `deepdive compile`:

* `app.ddlog`
* `deepdive.conf`
* `schema.json`

`deepdive compile` should be rerun whenever changes to these files are to be reflected on the execution done by `deepdive do`.
Otherwise, the modifications in `app.ddlog` won't be considered, and simply the last compiled code will execute.


## Why compile

Compiling a DeepDive application presents two main advantages.
First, it helps to run the application faster by extracting the correct dependencies between the different operations.
Second, it provides many checks for the application and helps detecting errors even before running DeepDive.


## Compile-time checks

All the extractors and tables declarations can be written in any order in `app.ddlog`: the whole dataflow is extracted during the compilation and many checks are made.
All these checks can also be performed individually by the command `deepdive check`.
In particular, the following checks are made:

* `input_extractors_well_defined`: checks sanity of all defined extractors.
* `input_schema_wellformed`: checks if schema variables are well formed.
* `compiled_base_relations_have_input_data`: all the tables declared that are not filled by an extractor must have input data in `input/` that can be [loaded automatically](ops-data.md#organizing-input-data).
* `compiled_dependencies_correct`: check that the dependencies are correct. In particular, in the application includes extractors in deepdive.conf, each output relation must be output by exactly one extractor.
* `compiled_input_output_well_defined`: checks if all inputs and outputs of compiled processes are well-defined.
* `compiled_output_uniquely_defined`: checks if all outputs in the compiled documents are defined by one process.

