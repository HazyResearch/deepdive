# Compute Drivers
Compute driver abstracts how an extractor defined in a DeepDive application is executed on a particular computing resource, e.g., local machine, remote server, cluster with a job scheduler, or Hadoop cluster.
A DeepDive application can be configured to run its extractors on multiple computing resources as long as the user code is written with care not assuming a particular setup.
Operators defined by each compute driver are used by extractor execution plans compiled from `deepdive.extraction.extractor` blocks defined in the DeepDive application's deepdive.conf.

## Compiled Extractor
(TODO)

Every extractor defined in deepdive.conf is compiled into a filesystem directory that consists of the following set of files, regardless of its style (tsv, json, sql, cmd):

* `prepare.sh`
* `start.sh`
* `stop.sh`
* `resume.sh`
* `cleanup.sh`

These corresponds to a ...

In these execution plan scripts, ...

## Core Compute Driver Operators
Every compute driver must implement the following operators.

1. `compute-prepare-input SQL`
    * Prepares the input data for the extractor.
2. `compute-start`
    * Starts the execution of the extractor.
3. `compute-is-running`
    * Starts the computation.
4. `compute-has-completed`
5. `compute-restart-failed`
6. `compute-collect-output TABLE`

7. `compute-stop`
8. `compute-cleanup`
