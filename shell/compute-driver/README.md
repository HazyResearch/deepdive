# Compute Drivers
Compute driver abstracts how an extractor defined in a DeepDive application is executed on a particular computing resource, e.g., local machine, remote server, cluster with a job scheduler, or Hadoop cluster.
A DeepDive application can be configured to run its extractors on multiple computing resources as long as the user code is written with care not assuming a particular setup.
Operators defined by each compute driver are used by extractor execution plans compiled from `deepdive.extraction.extractor` blocks defined in the DeepDive application's deepdive.conf.

## Core Compute Driver Operators
Every compute driver must implement the following operators.

1. `compute-prepare-input SQL`
    * Prepares the input data for the extractor by running `SQL` against the database.
2. `compute-prepare-code FILE/COMMAND`
    * Prepares the code for the extractor.
3. `compute-has-completed`
    * Checks whether all computation has completed.
4. `compute-start-remaining`
    * Starts the execution of the extractor for parts of the computation that have not run yet.
    * Restarts any failed parts.
5. `compute-collect-output TABLE`
    * Collects completed parts of computation and loads it to `TABLE` in the database.

6. `compute-stop`
    * Stops the execution.
7. `compute-cleanup`
    * Cleans up any temporary state after completion.

8. `compute-is-running`
    * Checks whether the extractor is running.
9. `compute-progress`
    * Enumerates all parts of computation and their states: completed/running/waiting.
10. `compute-reset`
    * Resets the state, so the extractor can be run from scratch again.


## Compiled Extractor

Every extractor defined in deepdive.conf is compiled into a filesystem directory that consists of the following set of files, regardless of its style (tsv, json, sql, cmd):

* `run.sh`

### Extractor with a UDF or Command
For example, a `tsv_extractor` with an input SQL and a Python UDF script will be compiled into the following script:

```bash
#!/usr/bin/env bash
# run/RUNNING/extractors/example_tsv_extractor/run.sh
cd "$(dirname "$0")"
compute-prepare-input "$input_query"
compute-prepare-code "$udfOrCmd"

trap compute-stop EXIT
while ! compute-has-completed; do
    compute-start-remaining
    sleep $DEEPDIVE_COMPUTER_POLL_INTERVAL
    compute-collect-output "$output_relation"
done
compute-stop
trap - EXIT

compute-cleanup
```

### Extractor with only SQL

```bash
#!/usr/bin/env bash
# run/RUNNING/extractors/example_sql_extractor/run.sh
cd "$(dirname "$0")"
db-execute "$sql"
```
