---
layout: default
---

# DeepDive Application

## Structure

A DeepDive application is a directory that contains the following files and directories:

* `deepdive.conf`

    Extractors, and inference rules are written in [HOCON][] syntax in this file.
    See the [Configuration Reference](http://deepdive.stanford.edu/doc/basics/configuration.html) for full details.

* `db.url`

    A URL representing the database configuration is supposed to be stored in this file.
    For example, `postgresql://user:password@localhost:5432/database_name` can be the line stored in it.

* `schema.sql`

    Data-Definition Language (DDL) statements for setting up the underlying database tables should be kept in this file.

* `input/`

    Any data to be processed by this application is suggested to be kept under this directory.

    * `load.sh`

        In addition to the data files, there should be an executable script that knows how to load the data here to the database once its tables are created.

* `udf/`

    Any user-defined function (UDF) code is suggested to be kept under this directory.
    They can be referenced from deepdive.conf with path names relative to the application root.

* `run/`

    Each run of the DeepDive application has a corresponding subdirectory under this directory whose name contains the timestamp when the run was started, e.g., `run/20150618-223344.567890/`.
    All output and log files that belong to the run are kept under that subdirectory.
    There are a few symbolic links with mnemonic names to the most recently started run, last successful run, last failed run for handy access.

[HOCON]: https://github.com/typesafehub/config/blob/master/HOCON.md#readme "Human Optimized Configuration Object Notation"


## Operations

There are several operations that are frequently performed on a DeepDive application.
Any of the following command can be run under any subdirectory of a DeepDive application to perform a certain operation.

### Initializing Database

```bash
deepdive initdb
```

This command initializes the underlying database configured for the application by creating necessary tables and loading the initial data into them.
It makes sure the following:

1. The configured database is created.
2. The tables defined in `schema.sql` are created.
3. The data that exist under `input/` are loaded into the tables with the help of `load.sh`.


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


### Running SQL Queries

```bash
deepdive sql
```

This command opens a SQL prompt for the underlying database configured for the application.

Optionally, the SQL query can be passed as a command line argument to run and print its result to standard output as tab-separated values.
For example, the following command prints the number of sentences per document:

```bash
deepdive sql "SELECT doc_id, COUNT(*) FROM sentences GROUP BY doc_id"
```

