---
layout: default
---

# Writing a new DeepDive application

This document describes how to create a new application that uses DeepDive to
analyze data. 

This task is composed by a number of steps:

1. Creating the application skeleton 
2. Configuring the database connection
3. Importing the data
4. Writing extractors
5. Writing the inference schema
6. Writing inference rules
7. Testing

### 1 - Creating the application skeleton

We start by creating a new folder `app/testapp` in the `deepdive` directory. All
files for our application will reside in this directory. 

```bash
mkdir -p app/testapp 
cd app/testapp
```

DeepDive's main entry point is a file called `application.conf` which contains
the database connection information as well as the specification for feature
extraction and for specifying the schema and the inference rules for the factor
graph. It is often useful to have a small 'env.sh' script to specify
environmental variables and a `run.sh` script that loads those variables and
executes the DeepDive pipeline. The DeepDive distribution provides simple
templates for both of these scripts and for the `application.conf` file. We copy
these templates to our directory with the following commands: 

```bash
cp ../../examples/template/application.conf .
cp ../../examples/template/run.sh .
cp ../../examples/template/env.sh .
```

In `env.sh` there is a placeholder line `export DBNAME=` that must be modified
to contain the name of the database used by the application: 

```bash
# File: env.sh
...
export DBNAME=deepdive_testapp 
...
```

Make sure the database is created. Execute:
```bash
createdb deepdive_testapp
```

We can try executing the `run.sh` script now to verify that everything is
working correctly:

```bash
./run.sh
```
Since we have not defined any extractors or inference rules, the results will
not be interesting, but DeepDive should run successfully from end to end. If
this is the case, the summary report should look like this: 

    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  Summary Report
    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  --------------------------------------------------

### 2 - Configuring the database connection

We define the connection to the PostgreSQL instance in the `application.conf`
file. The URL of the instance should be specified in [JDBC
format](http://jdbc.postgresql.org/documentation/80/connect.html). A username
and password can also be specified:
    
```bash
    deepdive: {
      db.default {
        driver   : "org.postgresql.Driver"
        url      : "jdbc:postgresql://[host]:[port]/[database_name]"
        user     : "deepdive"
        password : ""
        dbname   : [database_name]
        host     : [host]
        port     : [port]

      }
    }
```

For advanced connection pool options refer to the [Scalikejdbc
configuration](http://scalikejdbc.org/documentation/configuration.html).

### <a name="loading" href="#"></a> 3 - Importing the data

DeepDive assumes that the schema for the application has been created in the
database. This means that all relations used by any of the extractors and inference
rules must exist before running DeepDive. It is recommended to do this in a data
preparation script, as shown in the [example
walkthrough](walkthrough/walkthrough.html) or in the examples that ship with
deepdive. It is **mandatory** for **all relations** that will contain variables
to have a **unique primary key called `id`**. If these tables are populated by
an [extractor](extractors.html), the extractor should fill the `id` column with
`NULL` values.

### <a name="extractors" href="#"></a> 4 - Writing extractors 

DeepDive supports [multiple types of extractors](extractors.html) to perform
[feature extraction](overview.html#extractors). The output of an extractor is
written back to the data store by DeepDive, and can be used in other extractors
and/or during the inference step. Users can also specify extractors that simply
execute SQL queries or an arbitrary shell commands. The ['Writing
extractors'](extractors.html) document contains an in-depth description of the
available types of extractors complete with examples.

###<a name="schema" href="#"></a> 5 - Writing the inference schema
The schema is used to define the [query variable nodes of the factor
graph](../general/inference.html#variables). Each variable has a data type
associated with it. Currently, DeepDive supports Boolean variables and
[Multinomial/Categorical variables](schema.html#multinomial). See the ['Defining
inference variables in the schema'][schema.html] for more information and
examples.

### <a name="inference" href="#"></a> 6 - Writing inference rules

DeepDive exposes a language to easily build factor graphs by writing *rules*
that define the relationships between variables. For example, the following rule
states that if a person smokes, he or she is likely to have cancer, and that the
weight of the rule should be learned automatically based on training data
(special value '?'):

    smokesFactor {
      input_query : """SELECT * from people"""
      function    : "Imply(people.smokes, people.has_cancer)"
      weight      : ?
    }

DeepDive's language can express complex relationships that use extracted
features. Refer to the [guide for writing inference rules](inference_rules.html)
and to the ['Inference Rule Function Reference'](inference_rule_functions.html)
for in-depth information about writing inference rules.

### 7 - Running, testing, and evaluating the results

For details about running an application and querying the results see the
[appropriate document](running.html). Writing an application is an iterative
process that requires progressive specification and refinements of extractors,
schema, and inference rules. DeepDive tries to simplify this task by providing
*calibration data* and plots, as explained in the [calibration
guide](calibration.html). While testing extractors and inference rules, it can be
useful to execute only a subset of them. This is possible by [configuring
pipelines](running.html#pipelines). 

