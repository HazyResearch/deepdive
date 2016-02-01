---
layout: default
title: Controlling execution of data processing
---

# Controlling execution of data processing


<todo>migrate what's below</todo>

## Operations

There are several operations that are frequently performed on a DeepDive application.
Any of the following command can be run under any subdirectory of a DeepDive application to perform a certain operation.

To see all options for each command, such as specifying alternative configuration file for running, see the online help message with the `deepdive help` command.  For example:

```bash
deepdive help run
```

### Initializing Database

```bash
deepdive initdb [TABLE]
```

This command initializes the underlying database configured for the application by creating necessary tables and loading the initial data into them.
If `TABLE` is not given, it makes sure the following:

1. The configured database is created.
2. The tables defined in `schema.sql` (for deepdive application) or `app.ddlog` (for ddlog application) are created.
3. The data that exists under `input/` is loaded into the tables with the help of `init.sh`.

If `TABLE` is given, it will make sure the following:

1. The configured database is created.
2. The given table is created.
3. The data that exists under `input/` is loaded into the `TABLE` with the help of `init_TABLE.sh`.


### Running Pipelines

```bash
deepdive run
```

This command runs the `default` pipeline defined in `deepdive.conf`.
It creates a new directory for the run under `run/`, which is first pointed by `run/RUNNING`, then pointed by `run/LATEST` after the run finishes successfully, or by `run/ABORTED` when it was unsuccessfully finished.

Optionally, the name of the pipeline can be specified as a command line argument to run it instead.
For example, the following command runs `my_pipeline` instead of `default`:

```bash
deepdive run my_pipeline
```
