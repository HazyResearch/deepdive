---
layout: default
---

# Application configuration file reference

This document contains the description of each configuration directive that can
be specified in an application configuration file.

## Structure

<!-- TODO (MR) Describe blocks/section, format of variables, type of variables,
when to use "xxx", when """xxx xxx" when nothing ? -->

## Comments

Any text appearing after a `#` character is considered a comment.
<!-- TODO (MR) is this true ? -->

## Global section

All application configuration directives described in the rest of the document
must appear inside a global `deepdive` section:

```bash
	deepdive {
     # All configurations directive go here
    }

## <a name="database" href="#"></a> Database connection

The configuration directives for the database connection must be specified
inside a `db.default` section:

```bash
	deepdive {
	  db.default {
	    # Database connection parameters
	  }
	}
```

The configuration directives for the database connection are the following:

- `driver`: specify the JDBC driver to use. Currently only PostgreSQL is
  supported, so it must be "org.postgresql.Driver":
<!-- TODO (MR): must? -->

  ```bash
	driver   : "org.postgresql.Driver"
  ```

- `url`: the URL of the database instance in [JDBC
  format](http://jdbc.postgresql.org/documentation/80/connect.html):

  ```bash
	url      : "jdbc:postgresql://[host]:[port]/[database_name]" 
  ```

<!-- TODO (MR) Is the following correct or do we need both ? -->
  Alternatively, the instance can be specified through the `host`, `port`, and
  `dbname` directives:

  ```bash
    host     : [host]
    port     : [port]
	dbname   : [database_name]
  ```

- `user`: the database user:

  ```bash
    user     : "deepdive"
  ```

- `password`: the password for the database user

  ```bash
	password : "dbpassword"
  ```

<!-- TODO (MR) Anything else for the DB connection ? -->

## <a name="extraction" href="#"></a> Extraction and extractors

Configuration directives for executing [extractors](extractors.html) and
extractor definitions go in the `extraction.extractors` section:

```bash
deepdive {
  # ...
  extraction.extractors {
    # extraction directives and extractor definitions
  }
  # ...
}
```

There is currently only one available extraction configuration directive:

- `parallelism`: specifies the maximum number of extractors to execute in
  parallel <!-- TODO (MR) what is the default value ? -->

### <a name="extractor" href="#"></a> Extractors definition

Each extractor definition is a section named with the name of the extractor:
  ```bash
  deepdive {
    # ...
    extraction.extractors {
	  
	  extractor1 {
	    # definition of extractor1 
	  }
	  
	  extractor2 {
	    # definition of extractor2
	  }

	  # More extractors ...
    }
    # ...
  }
  ```

Different styles of [extractors](extractors.html) are defined using different
sets of directives. There is nevertheless a subset of directives that are common
to all styles:

- `style`: specifies the style of the extractor. Can take the values
  `json_extractor`, `tsv_extractor`, `plpy_extractor`, `sql_extractor`, or
  `cmd_extractor`. See the ['Writing extractors' document](extractors.html) for
  details about the different styles of extractors. This is a mandatory
  directive.

- `before`: specifies a shell command to run **before** executing the extractor.
  This is an optional directive.

  ```bash
    myExtractor {
	  # ...
	  style: "tsv_extractor"
	  # ...
      before: """echo starting myExtractor"""
	  # ...
	}
  ```

- `after`: specifies a shell command to run **after** the extractor has
  completed:

  ```bash
    myExtractor {
	  # ...
	  style: "sql_extractor"
	  # ...
      after: """python cleanup_after_myExtractor.py"""
	  # ...
	}
  ```

- `dependencies`: takes an array of extractor names that this extractor depends
  on. The system resolves the dependency graph and execute the extractors in the
  required order (in parallel if possible and `parallelism` has a value grater
  than 1). E.g.:

  ```bash
	extractor1 {
	  # ...
	}

	extractor2 {
	  # ...
	}

    myExtractor {
	  # ...
	  style: "cmd_extractor"
	  # ...
	  dependencies: [ "extractor1", "extractor2" ]
	  # ...
	}
  ```
  <!-- TODO (MR) Is this the correct way of specifying multiple dependencies ?
  Why do we need the "" here but not in the pipeline specification ? -->

  If an extractor specified in dependencies does not exist or is not in the
  active [pipeline](pipelines.html), that extractor will be ignored.  

The following directives are only for the `json_extractor`, `tsv_extractor`, and
`plpy_extractor` styles. They are **mandatory** for these styles.

- `input`: specifies the input to the extractor. For all the three extractor
  style above it can be a SQL query to run on the database, e.g.,:

  ```bash
    myExtractor {
	  # ...
	  style: "tsv_extractor"
	  # ...
	  input: """SELECT * FROM titles"""
	  # ...
	}
  ```

  **Only* for extractors with style `json_extractor`, the `input` directive may
  specify a TSV or CSV file to use as input, e.g.,:

  ```bash
    myExtractor {
	  # ...
	  style: "json_extractor"
	  # ...
	  input: CSV(pathtofile.csv) # or TSV(pathtofile.tsv)
	  # ...
	}
	<!-- TODO (MR) Is the above correct ? -->
  ```

- `output_relation`: specifies the name of the relation the extractor output
  should be written to. Must be an existing relation in the database. E.g.:

  ```bash
  ```bash
    myExtractor {
	  # ...
	  style: "plpy_extractor"
	  # ...
	  output_relation: words
	  # ...
	} 
  ```

- `udf`: specifies the extractor User Defined Function (UDF). This is a shell
  command or a script that is executed. Refer to the ['Writing extractors'
  guide](extractors.html) for details about the requirements for the UDF for
  different styles of extractors.

In the following subsections we present the directives that are specific to each
extractor style, if any.

#### <a name="json" href="#"></a> `json_extractor`

- `parallelism`: specifies the number of instances of this extractor to be
  executed in parallel. Tuples generated by the `input` query are sent (in
  batches of size controlled by the `input_batch_size` directive) to each
  instance in a round-robin fashion. This is an optional directive. Example
  usage: 

  ```bash
    myExtractor {
	  # ...
	  style: "json_extractor"
	  # ...
	  parallelism: 5
	  # ...
	}
  ```

- `input_batch_size`: specifies the size of the batch of tuples to feed to each
  instance of the extractor at each round-robin iteration. The default value is
  1000. This is an optional directive. Example usage:

  ```bash
    myExtractor {
	  # ...
	  style: "json_extractor"
	  # ...
	  parallelism: 5
	  # ...
	  input_batch_size: 5000
	  # ...
	}
  ```

- `output_batch_size`: specifies how many tuples in the output of the extractor
  are inserted into the `output_relation` at once. If the tuples are large, a
  smaller value may avoid incurring in out of memory errors. The default value
  is <!-- TODO (MR) what is the default value? -->. This is an optional
  directive. Example usage:

  ```bash
    myExtractor {
	  # ...
	  style: "json_extractor"
	  # ...
	  output_batch_size: 5000
	  # ...
	}
  ```

#### `sql_extractor`

- `sql`: specifies the SQL command to execute. This option is mandatory for this
  extractor style. Example usage:

  ```bash
    myExtractor {
	  style: "sql_extractor"
	  sql: """INSERT INTO titles VALUES (1, 'Moby Dick')"""
	}
  ```

#### `cmd_extractor`

- `cmd`: specifies the shell command to execte. This option is mandatory for
  this extractor style. Example usage:

  ```bash
    myExtractor {
	  style: "cmd_extractor"
	  cmd: """python words.py"""
	}
  ```

## <a name="inference-opt" href="#"></a> Inference 

*Note:* this section presents configuration directive for the inference step.
Refer to the [appropriate section](#inferencerules) for the directives to define
inference rules.

Configuration directives to control the inference steps go in the global
`deepdive` section. The available directives are:

- <a name="batch_size" href="#"></a> `inference.batch_size`: batch size to
  insert variables, factors, and weights in the database during the factor graph
  creation: <!-- TODO (MR) When and where ? -->

  ```bash
    inference.batch_size = 1000000 
  ```

  The default value depends on the used datastore (50000 for PostgreSQL).

- <a name="skip_learning" href="#"></a> `inference.skip_learning`: if `true`,
  DeepDive will skip the learning step for the factor weights and reuse the
  weights learned in the last execution. It will generate a table
  `dd_graph_last_weights` containing all the weights.  Weights will be matched
  by description, and no learning will be performed.  <!-- TODO (MR) What is
  `description`? -->

  ```bash
      inference.skip_learning: true
  ```
  By default this directive is `false`.

- <a name="weight_table" href="#"></a> `inference.weight_table`: to be used in
  combination with `inference.skip_learning`, it allows to skip the weight
  learning step and use the weights specified in a custom table. The table
  tuples must contain the factor description and weights
  <!-- TODO (MR) what does the above mean? What is the schema of this table ?
  -->
  This table can be the result from one execution of DeepDive (an example would
  be the view `dd_inference_result_variable_mapped_weights`, or
  `dd_graph_last_weights` used when `inference.skip_learning` is `true`) or
  manually assigned, or a combination of the two.

  If `inference_skip_learning` is `false` (default) this directive is ignored
  <!-- TODO (MR) is that true? -->

  ```bash
      inference.skip_learning: true
      inference.weight_table: [weight table name]
  ```

<!-- TODO (MR) Are there other inference directives? -->

## <a name="schema" href="#"></a> Inference schema

Inference schema directives define the variables used in the factor graph and
their type. Inference schema directives go in the `schema.variables` section:

  ```bash
    deepdive {
	  # ...
	  schema.variables {
	    # Variable definitions
	  }
	  # ...
  }
  ```

A variable is defined by its name and its type:

   ```bash
	 people.smoke: Boolean
	 people.has_cancer: Boolean
   ```

DeepDive currently supports only Boolean variables.

## <a name="inference_rules" href="#"></a> Inference rules

*Note:* refer to ['Writing inference rules' document](inference_rules.html) for
an in-depth discussion about writing inference rules.

The definitions of inference rules for the factor graphs go in the
`inference.factors` section:

```bash
    deepdive {
      inference.factors: {
	    rule1 {
		  # definition of rule1
	    }

		rule2 {
		  # definition of rule2 
		}
		
		# more rules...
	  }
	}
```

The **mandatory** definition directives for each rule are:

- `input_query`: specifies the variables to create. It is a SQL query that
  usually combines relations created by the extractors. For each row in the
  query result, the factor graph will have variables for a subset of the columns in
  that row, one variable per column, all connected by a factor. The output of
  the `input_query` must include the reserved `id` column for each variable.

- `function`: specifies the factor function and the variables connected by the
  factor. Refer to the [Inference rule function
  reference](inference_rule_functions.html) for details about the available
  functions. Example usage:

- `weight`: specifies whether the weight of the factor should be a specified
  constant or learned (and if so, whether it should be a function of some
  columns in the input query. Possible values for this directive are:

  - a real number: the weight is the given number and not learned.
  
  - "?": DeepDive learns a weight for all factors defined by this rule. All the
	factors will share the same weight.

  - "?(column_name)": DeepDive learns multiple weights, one for each different
	value in the column `column_name` in the result of `input_query`. 

  <!-- TODO (MR) what is the "type" of this variable ? -->

An example inference rule is the following:

	```bash
	  myRule {
        input_query : """
          SELECT people.id         AS "people.id",
                 people.smokes     AS "people.smokes",
                 people.has_cancer AS "people.has_cancer",
                 people.gender     AS "people.gender"
          FROM people
          """
        function    : "Imply(people.smokes, people.has_cancer)"
        weight      : "?(people.gender)"
      }
	  ```

<!-- TODO (MR) Anything else in the inference rule definition ? -->

## <a name="pipelines" href="#"></a> Pipelines

[Pipelines](running.html#pipelines) specification directives go in the global `deepdive`
section. Available directives are:

- `pipeline.pipelines`: This is a section containing a list <!-- TODO (MR) how
  is it specified ? separated by commas ? -->
  of one or more pipelines. Each pipeline is defined as an array of tasks, where
  the names of the tasks are the names of the extractors or inference rules to
  be executed, as in the following example:

  ```bash
    pipeline_name: [ extractor1 extractor2 inferenceRule1 ... ]
  ```

  For example:

  ```bash
    pipeline.pipelines { myPipeline: [ extractor1 extractor2 inferenceRule1 ] } 
  ```

  <!-- TODO (MR) Is the above syntax correct? The walkthrough uses an example
  like:
pipeline.pipelines.withnlp: [
  "ext_sentences",    # NLP extractor, takes very long
  "ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features",
  "f_has_spouse_features", "f_has_spouse_symmetry"
]

pipeline.pipelines.nonlp: [
  "ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features",
  "f_has_spouse_features", "f_has_spouse_symmetry"
]

Is it the same? 
-->

  Notice that:

  - If there is no extractor or inference rule with a specified name, that
	name is ignored. 

  - If an extractor specified in the pipeline contains in its `dependencies`
	directive another extractor **not** specified in the pipeline, this
	particular dependency is ignored.

  - When no inference rules are active in the pipeline, DeepDive only execute
	the extractors, skipping the weight learning and inference steps.

- `pipeline.run`: specify which pipeline to run, e.g.:
  
  ```bash
    pipeline.run : myPipeline
  ```
  
  Only a single pipeline can be specified. When the `pipeline.run` directive is
  not specified, DeepDive executes all extractors and inference rules. 

- `pipeline.relearn_from`: this directive takes as value a path to a previous
  execution of the application. It instructs DeepDives to **skip all
  extractors** and use the factor graph contained in the specified directory for
  the learning and inference steps. Usage example:

  ```bash
    pipeline.relearn_from: "/PATH/TO/DEEPDIVE/HOME/out/2014-05-02T131658/"
  ```
<!-- TODO (MR) Other pipeline directives ? -->

## <a name="sampler" href="#"></a> Sampler 
Configuration directives for the sampler go in the global `deepdive` section.
The available directive are:

- `sampler.sampler_cmd`: the path to the sampler executable:

  ```bash
      sampler.sampler_cmd: "util/sampler-dw-mac gibbs"
  ```
  <!-- TODO (MR) The above seems to also include a parameter `gibbs`. Why is
  `gibbs` not in `sampler.sampler_args` ? -->

  Since [version 0.03](../changelog/0.03-alpha.html), DeepDive automatically
  chooses the correct executable based on your environment, so we recommend to
  omit the `sampler_cmd` directive.
  <!-- TODO (MR) Does the above mean that we always use DimmWitted? Are there
  other samplers? What is s the interface of this command? What arguments _must_
  accept ? -->

- `sampler.sampler_args`: the arguments to the sampler executable:

  ```bash
    deepdive {
      sampler.sampler_args: "-l 1000 -s 1 -i 1000 --alpha 0.01"
    }
  ```

  <!-- TODO (MR) What are the default values ? -->

  For a list and the meaning of the arguments, please refer to the [documentation of our
  DimmWitted sampler](sampler.html).

  <!-- TODO (MR) Are there other sampler directives ? -->

## Calibration / Holdout

Directive for [calibration](calibration.html) go to the `calibration` section.
The available directives are:

- `holdout_fraction`: specifies the fraction of training data to use for
  [holdout](calibration.html#holdout). E.g.:

	```bash
		calibration: {
		  holdout_fraction: 0.25
		}
	```
- `holdout_query`: specifies a custom query to be use to define the holdout set.
  The must insert all variable IDs that are to be held out into the
  `dd_graph_variables_holdout` table through arbitrary SQL. E.g.:
 
	```bash
		calibration: {
		  holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM mytable WHERE predicate"
		}
	```

  When a custom holdout query is defined in `holdout_query`, the
  `holdout_fraction` setting is ignored. 

<!-- TODO (MR) Are there other directives ? -->

