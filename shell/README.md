# DeepDive Application Life Cycle

This document describes the artifacts and user commands involved in the life cycle of a DeepDive application.

## DeepDive Artifacts
There are largely three groups of artifacts involving a DeepDive application.

### 1. Code

First of all, DeepDive application code defines the data schema, the data transformations and dependencies between them, and how the transformed data maps to the statistical model.
DeepDive distinguishes source code from compiled executable code.

#### Source Code
The source code refers to the rules written by the user in DDlog and DeepDive configuration syntax as well as the UDF programs written in Python or the user's language of choice.

DDlog code is expected to be kept in `app.ddlog`, and all configuration blocks must be present in `deepdive.conf`.
UDF code is expected to be kept under `udf/`.

#### Executable Code
In order to run the application, DeepDive compiles the user's source code into code that is readily executable, such as shell scripts and Makefiles.

The compiled code is kept under `run/process/`.


### 2. Data
DeepDive applications transform input data to construct a machine learning model.

#### Input Data
A DeepDive application has a collection of *input data* that falls into the following two classes:

* Raw unstructured/semi-structured data to extract structured data from, such as text corpora, tables, diagrams, and images;
* Structured data used by the code to drive the extraction, such as dictionaries, ontologies, and existing (incomplete) knowledge bases.

In either case, the serialized form of the input data or the executable that generates it is expected to be kept under `input/`.

#### Current Working Database
DeepDive assumes all processed data as well as input data are accessible through a relational database.
Whether the data is stored in an actual RDBMS is not important.
What matters is the fact that all data DeepDive touches must have a clear relational schema.
The *current working database* is where all input data will be read from and all processed data will be stored to.
All data transformations and model generation driven by the code mutate the current working database.

The last modified timestamp of every relation in the current working database is kept under `run/data/`.


#### Database Snapshots
DeepDive provides a way to record the state of the current working database and switch back to a particular point in time if it's recorded as a *snapshot*.
DeepDive snapshots heavily rely on PostgreSQL's "schema" support, and other database drivers may not support snapshots efficiently, resulting in full backups and restores.


### 3. Models
A DeepDive application ultimately constructs a machine learning model.

#### Current Working Model
Similar to the current working database, a DeepDive application has a *current working model* to which all relevant operations are applied, e.g., grounding, learning/training, and inference/prediction/testing.

#### Saved Models
Similar to snapshots, the current working model can be saved when there's a need to keep the current working model.


## DeepDive User Commands

### Code Compilation
#### `deepdive compile`
Compiles source code of DeepDive application into executable code.
Most compile-time error checks against `deepdive.conf` and `app.ddlog` are done at this step.

The compiled code is kept under the path `run/process/`.
Each extractor defined either directly in `deepdive.conf` or implicitly in `app.ddlog` is compiled as an individually executable, standalone program that can be run under its own working directory.
The dependency between these programs are encoded as GNU Make rules and targets in `run/CURRENT/Makefile` that also keeps track of their last executed timestamps.

#### `deepdive codegen UDF`
Generates skeleton source code for functions (UDFs) declared in DDlog under `udf/`.

#### `deepdive typecheck UDF`
Checks source code for given function declared in DDlog with synthetic data.

### Data Processing
#### `deepdive do SOMETHING...`
#### `deepdive mark STATE SOMETHING...`
#### `deepdive redo SOMETHING...`
#### `deepdive plan SOMETHING...`

### Database Management
#### `deepdive initdb`
#### `deepdive import RELATION SOURCE`
#### `deepdive export RELATION SINK`
#### `deepdive clear RELATION...`

### Database Versioning
TODO snapshot -> db, stage, ?
#### `deepdive snapshot create`
#### `deepdive snapshot list`
#### `deepdive snapshot switch`
#### `deepdive snapshot drop`

### Statistical Modeling
#### `deepdive variables`
#### `deepdive factors`
#### `deepdive model ground VARIABLE/FACTOR...`
#### `deepdive model list`
#### `deepdive model switch`
#### `deepdive model drop`
#### `deepdive model learn`
#### `deepdive model infer`

### Browsing / Labeling / Rule Engineering
#### `deepdive view`

### Shorthands
#### `deepdive run`
#### `deepdive sql`
#### `deepdive sql eval SQL`
