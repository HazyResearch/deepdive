---
layout: default
---

# Writing a new DeepDive application

This document describes how to create a new application that uses DeepDive to
analyze data.

This task is composed by a number of steps:

1. Creating the application skeleton
2. Loading the data
3. Writing extractors
4. Writing the inference schema
5. Writing inference rules
6. Running, testing, and evaluating the results


### 1. Creating the application skeleton

We start by creating a new folder `app/testapp` in the `deepdive` directory. All
files for our application will reside in this directory.

```bash
mkdir -p app/testapp
cd app/testapp
```

The DeepDive source tree provides simple templates for the `deepdive.conf` file.
We copy these templates to our directory with the following commands:

```bash
cp -av ../../examples/template/. .
```

The database connection for the application can be configured in the `db.url` file, for example, to use the `deepdive_testapp` database on a local PostgreSQL installation:
```bash
echo postgresql://localhost/deepdive_testapp  >./db.url
```

The following command initializes an empty database:
```bash
deepdive initdb
```

Then we can run the following command to verify that everything is
working correctly:

```bash
deepdive run
```
Since we have not defined any extractors or inference rules, the results will
not be interesting, but DeepDive should run successfully from end to end. If
this is the case, the summary report should look like this:

    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  Summary Report
    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  --------------------------------------------------


### <a name="loading" href="#"></a> 2. Loading the data

DeepDive assumes the table schema for the application is defined in the `schema.sql` file as `CREATE TABLE` statements.
All tables used by any of the extractors and inference rules must be created by this file.
It is **mandatory** for **all tables** that will contain variables to have a **unique primary key called `id`**.
If these tables are populated by an [extractor](extractors.html), the extractor should fill the `id` column with `NULL` values.
Any data that should be populated for the extractors should be loaded by the `input/init.sh` script as [demonstrated in the walkthrough](walkthrough/walkthrough.html#loading_data).


### <a name="extractors" href="#"></a> 3. Writing extractors

DeepDive supports [multiple types of extractors](extractors.html) to perform
[feature extraction](overview.html#extractors). The output of an extractor is
written back to the data store by DeepDive, and can be used in other extractors
and/or during the inference step. Users can also specify extractors that simply
execute SQL queries or an arbitrary shell commands. The ['Writing
extractors'](extractors.html) document contains an in-depth description of the
available types of extractors complete with examples.


###<a name="schema" href="#"></a> 4. Writing the inference schema

The schema is used to define the [query variable nodes of the factor
graph](../general/inference.html#variables). Each variable has a data type
associated with it. Currently, DeepDive supports Boolean variables and
[Multinomial/Categorical variables](schema.html#multinomial). See the ['Defining
inference variables in the schema'](schema.html) for more information and
examples.


### <a name="inference" href="#"></a> 5. Writing inference rules

DeepDive exposes a language to easily build factor graphs by writing *rules*
that define the relationships between variables. For example, the following rule
states that if a person smokes, he or she is likely to have cancer, and that the
weight of the rule should be learned automatically based on training data
(special value '?'):

```bash
smokes_cancer {
  input_query: """
      SELECT person_has_cancer.id as "person_has_cancer.id",
             person_smokes.id as "person_smokes.id",
             person_smokes.smokes as "person_smokes.smokes",
             person_has_cancer.has_cancer as "person_has_cancer.has_cancer"
        FROM person_has_cancer, person_smokes
       WHERE person_has_cancer.person_id = person_smokes.person_id
    """
  function: "Imply(person_smokes.smokes, person_has_cancer.has_cancer)"
  weight: "?"
}
```

DeepDive's language can express complex relationships that use extracted
features. Refer to the [guide for writing inference rules](inference_rules.html)
and to the ['Inference Rule Function Reference'](inference_rule_functions.html)
for in-depth information about writing inference rules.


### 6. Running, testing, and evaluating the results

For details about running an application and querying the results see the
[appropriate document](running.html). Writing an application is an iterative
process that requires progressive specification and refinements of extractors,
schema, and inference rules. DeepDive tries to simplify this task by providing
*calibration data* and plots, as explained in the [calibration
guide](calibration.html). While testing extractors and inference rules, it can be
useful to execute only a subset of them. This is possible by [configuring
pipelines](running.html#pipelines).

