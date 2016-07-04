---
layout: default
title: Executing processes in the data flow
---

# Executing processes in the data flow

After [compiling a DeepDive application](ops-compiling.md), each compiled process in the data flow defined by the application can be executed with great flexibility.
DeepDive provides a core set of commands that supports precise control of the execution.
The following tasks are a few examples that are easily doable in DeepDive's vocabulary:

* Executing the complete data flow.
* Executing a fragment of the complete data flow.
* Stopping execution at any point and resuming from where it stopped.
* Repeating certain processes with different parameters.
* Skipping expensive processes by loading their output from external data sources.


## End-to-end execution

To simply run the complete data flow in an application from the beginning to the end, use the following command under any subdirectory of the application:

```bash
deepdive run
```

This command will:

1. Initialize the application as well as its configured database.
2. Run all processes that correspond to the [normal derivation rules](writing-dataflow-ddlog.md) as well as the [rules with user-defined functions](writing-udf-python.md).
3. Ground the factor graph according to the [inference rules defining the model for statistical inference](writing-model-ddlog.md).
4. Perform [weight learning and inference](ops-model.md) to compute marginal probability of every variable.
5. Generate [calibration plots and data](calibration.md) for debugging.



## Granular execution scenarios

There are several execution scenarios that frequently arise while developing a DeepDive application.
Any of the commands shown in this page can be run under any subdirectory of a DeepDive application.
To see all options for each command, the online help message can be seen with the `deepdive help` command.
For example, the following shows detailed usage of `deepdive do` command:

```bash
deepdive help do
```

### Partial execution

Running only a small part of the data flow defined in an application is the most common scenario.
The following command allows the execution to stop at given TARGETs instead of continuing to the end.

```bash
deepdive do TARGET...
```

It will present a shell script in a text editor that enumerates the processes to be run for the given TARGETs.
By saving a final plan and quiting the editor, the actual execution starts.

Valid TARGET names are shown when no argument is given.

```bash
deepdive do
```

Refer to the [next section](#compiled-processes-and-data-flow) for more detail about these TARGET names.


### Stopping and resuming

The execution started can be interrupted at any time (with Ctrl-C or ^C) or aborted by an error, then resumed later.

```bash
deepdive do TARGET...
```
```
...
^C
...
```
```bash
deepdive do TARGET...
```

DeepDive will resume the execution from the last unfinished process, skipping what has already been done.


### Repeating

Repeating certain parts of the data flow is another common scenario.
DeepDive provides a way to mark certain processes as not done so they can be repeated.
For example, the sequence of two commands below is basically what `deepdive run` does, i.e., repeats the end-to-end execution.

```bash
deepdive mark todo init/app calibration weights
```
```bash
deepdive do        init/app calibration weights
```

DeepDive provides a shorthand `deepdive redo` for easier repeating:

```bash
deepdive redo init/app calibration weights
```


### Skipping

Skipping certain parts of the data flow and starting from data manually loaded is also easily doable.
This can be useful to skip certain processes that take very long.
For example, suppose relation `bar_derived_from_foo` is derived from relation `foo`, and `foo` takes excessive amount of time to compute and therefore has been saved at `/some/data/source.tsv`.
Then the following sequence of commands skips all processes that `foo` depends on, assumes it is new, and executes the processes that derive `bar_derived_from_foo` from `foo`.

```bash
deepdive create table foo
```
```bash
deepdive load foo /some/data/source.tsv
```
```bash
deepdive mark new foo
```

```bash
deepdive do bar_derived_from_foo
```



## Compiled processes and data flow

For execution, DeepDive produces a compiled data flow graph that consists of primarily *data*, *model*, and *process* nodes and edges representing dependencies between them.

* Data nodes correspond to relations or tables in the database.
* Model nodes denote artifacts for statistical learning and inference.
* Process nodes represent a unit of computation.
* An edge between a process and data/model nodes mean the process takes as input or produces such node.
* An edge between two processes denotes one is dependent on another while hiding the details about the intermediary nodes.

DeepDive compiles a few built-in processes into the data flow graph that are necessary for initialization, statistical learning and inference, and calibration.
The rest of the processes correspond to the rules for deriving relations according to the [DDlog program](writing-dataflow-ddlog.md) and the [extractors in deepdive.conf](configuration.md#extraction-and-extractors).
Below is a data flow graph compiled for the [tutorial example](example-spouse.md), which can be found at `run/dataflow.svg` for any application.
[<img src="images/spouse/dataflow.svg">](images/spouse/dataflow.svg)


### Execution plan

DeepDive uses the compiled data flow graph to find the correct order of processes to execute.
Any node or set of nodes in the data flow graph can be set as the target for execution, and DeepDive enumerates all necessary processes as an *execution plan*.

#### deepdive plan

This execution plan can be seen using the `deepdive plan` command.
For example, when a plan for `data/sentences` is asked using the following command:

```bash
deepdive plan data/sentences
```

DeepDive gives an output that looks like:

```bash
# execution plan for sentences

: ## process/init/app ##########################################################
: # Done: 2016-02-01T20:56:50-0800 (2d 23h 55m 9s ago)
: process/init/app/run.sh
: mark_done process/init/app
: ##############################################################################
:
: ## process/init/relation/articles ############################################
: # Done: 2016-02-01T20:56:55-0800 (2d 23h 55m 4s ago)
: process/init/relation/articles/run.sh
: mark_done process/init/relation/articles
: ##############################################################################
:
: ## data/articles #############################################################
: # Done: 2016-02-01T20:56:55-0800 (2d 23h 55m 4s ago)
: # no-op
: mark_done data/articles
: ##############################################################################

## process/ext_sentences_by_nlp_markup #######################################
# Done: N/A
process/ext_sentences_by_nlp_markup/run.sh
mark_done process/ext_sentences_by_nlp_markup
##############################################################################

## data/sentences ############################################################
# Done: N/A
# no-op
mark_done data/sentences
##############################################################################
```

An execution plan is basically a shell script that invokes the actual run.sh compiled for each process.
Processes that have been already marked as done in the past are commented out (with `:`), and exactly when they were done is displayed in the comments.

#### deepdive do

DeepDive provides a `deepdive do` command that takes as input the target nodes in the data flow graph, presents the execution plan in an editor, then executes the final plan.
Because of the provided chance to modify the generated execution plan, the user has complete control over what is executed, and override or skip certain processes if needed.

```bash
deepdive do data/sentences
```

### Execution timestamps

After a process finishes its execution, the execution plan includes a `mark_done` command to mark the executed process as done.
The command touches a <code>*process/name*.done</code> file under the `run/` directory to record a timestamp.
These timestamps are then used for determining which processes have finished and which others have not.

#### deepdive mark

DeepDive provides a `deepdive mark` command to manipulate such timestamps to repeat or skip certain parts of the data flow.
It allows a given node to be marked as:

* `done`

    So all processes depending on the process including itself can be skipped.

* `todo-from-scratch`

    So all processes from the first process in the execution plan can be repeated.

* `todo`

    So all processes that depend on the node including itself can be repeated.

* `new`

    So all processes that depend on the node can be repeated (not including itself).

* `all-new`

    So all processes that depend on the node or any of its ancestor can be repeated (not including itself).

For example, if we mark a process as to be repeated using the following command:

```bash
deepdive mark todo process/init/relation/articles
```

Then `deepdive plan` will give an output like below to repeat the processes already marked as done in the past:

```bash
# execution plan for sentences

: ## process/init/app ##########################################################
: # Done: 2016-02-01T20:56:50-0800 (2d 23h 55m 39s ago)
: process/init/app/run.sh
: mark_done process/init/app
: ##############################################################################

## process/init/relation/articles ############################################
# Done: 2016-02-01T20:56:55-0800 (2d 23h 55m 34s ago)
process/init/relation/articles/run.sh
mark_done process/init/relation/articles
##############################################################################

## data/articles #############################################################
# Done: 2016-02-01T20:56:55-0800 (2d 23h 55m 34s ago)
# no-op
mark_done data/articles
##############################################################################

## process/ext_sentences_by_nlp_markup #######################################
# Done: N/A
process/ext_sentences_by_nlp_markup/run.sh
mark_done process/ext_sentences_by_nlp_markup
##############################################################################

## data/sentences ############################################################
# Done: N/A
# no-op
mark_done data/sentences
##############################################################################

```

### Built-in data flow nodes

DeepDive adds several built-in processes to the compiled data flow to ensure necessary steps are performed before and after the user-defined processes.

* `process/init/app`

    This process initializes the application's database and executes the `input/init.sh` script if available, which can be used for ensuring input data is downloaded and libraries and code required by UDFs are set up correctly.

    All processes that are not dependent on any other automatically becomes dependent on this process to ensure every process is executed after the application is initialized.

* <code>process/init/relation/*R*</code>

    Such process [creates the table *R* in the database and loads data from <code>input/*R*.*</code>](ops-data.md).
    These processes are automatically created and added to the data flow for every relation that is not derived from any other relation nor output by any process.

* `process/grounding/*`

    All processes for [grounding the factor graph](ops-model.md) are put under this namespace.

* `process/model/*`

    All processes for [learning and inference](ops-model.md) are put under this namespace.

* `model/*`

    All artifacts related to the statistical inference model that do not belong to the database are put under this namespace, e.g.:

    * `model/factorgraph`

        Denotes factor graph binary files under `run/model/factorgraph/`.

    * `model/weights`

        Denotes text files that contain learned weights of the model under `run/model/weights/`.

    * `model/probabilities`

        Denotes files holding the computed marginal probabilities under `run/model/probabilities/`

    * `model/calibration-plots`

        Denotes calibration plot images and data files under `run/model/calibration-plots/`.

* `data/model/*`

    All data relevant to statistical inference that go into the database are put under this namespace.
    For example, `data/model/weights` and `data/model/probabilities` correspond to [tables and views that keep the learning and inference results](reserved_tables.md).



## Environment variables

There are several environment variables that can be tweaked to influence the execution of processes for UDFs.

* `DEEPDIVE_NUM_PROCESSES`

    Controls the number of UDF processes to run in parallel.
    This defaults to one less than the number of processors, minimum one.

* `DEEPDIVE_NUM_PARALLEL_UNLOADS` and `DEEPDIVE_NUM_PARALLEL_LOADS`

    Controls the number of processes to run in parallel for unloading from and loading to the database.
    These default to one.

* `DEEPDIVE_AUTOCOMPILE`

    Controls whether `deepdive do` should automatically compile the app whenever a source file changes, such as `app.ddlog` or `deepdive.conf`.
    By default, it is `DEEPDIVE_AUTOCOMPILE=true`, i.e., automatically compiling to reduce the friction of manually running `deepdive compile`.
    However, it can be set to `DEEPDIVE_AUTOCOMPILE=false` to prevent unexpected recompilations.

* `DEEPDIVE_INTERACTIVE`

    Controls whether `deepdive do` should be interactive or not, asking to review the execution plan and allow editing before actual execution.
    By default, it is `DEEPDIVE_INTERACTIVE=false`, not asking whether to recompile the app upon source change, nor providing a chance to review or edit the execution plan.

* `DEEPDIVE_PLAN_EDIT`

    Controls whether a chance to edit the execution plan is provided (when set to `true`) or not (when `false`) in interactive mode.

* `VISUAL` and `EDITOR`

    Decides which editor to use.
    Defaults to `vi`.

