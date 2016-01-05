# Compute Drivers
Compute driver abstracts how an extractor defined in a DeepDive application is executed on a particular computing resource, e.g., local machine, remote server, cluster with a job scheduler, or Hadoop cluster.
A DeepDive application can be configured to run its extractors on multiple computing resources as long as the user code is written with care not assuming a particular setup.
Operators defined by each compute driver are used by extractor execution plans compiled from `deepdive.extraction.extractor` blocks defined in the DeepDive application's deepdive.conf.

## Core Compute Driver Operators
Every compute driver must implement the following operator.

1. `compute-execute input_query=SQL command=COMMAND output_relation=TABLE`


## Compiled Extractor

Every extractor defined in deepdive.conf is compiled into a filesystem directory that consists of the following set of files, regardless of its style (tsv, json, sql, cmd):

* `run.sh`

### Extractor with a UDF or Command
For example, a `tsv_extractor` with an input SQL and a Python UDF script will be compiled into the following script:

```bash
#!/usr/bin/env bash
# run/process/ext_example_tsv_extractor/run.sh
set -xeuo pipefail
cd "$(dirname "$0")"

compute-execute \
    input_query=... \
    command=... \
    output_relation=... \
    #
```

### Extractor with only SQL

```bash
#!/usr/bin/env bash
# run/RUNNING/extractors/example_sql_extractor/run.sh
cd "$(dirname "$0")"
db-execute "$sql"
```
