---
layout: default
title: deepdive.conf Reference
---

# Application configuration file reference

This document contains the description of each configuration directive that can
be specified in an application configuration file.

As a remark, from version [0.8](changelog/0.8.0-alpha.md), the user application is described in the [`app.ddlog` file](deepdiveapp.md). Therefore, the configuration file `deepdive.conf` described here doesn't have to be used. However, during a call of `deepdive compile`, the `app.ddlog` and `deepdive.conf` files are combined and compiled together. It is then possible to specify some parameters, arguments or tasks in the `deepdive.conf` file, in addition to the main structure of the application written in DDlog, and both of them will be considered by DeepDive.

## Overview of configuration structure

**Global section**: all application configuration directives described in the rest of the document
must appear inside a global `deepdive` section:

```
deepdive {
 # All configuration directives go here
}
```

A starter template of `deepdive.conf` is below. You can found it in
`examples/template/` in your `DEEPDIVE_HOME` installation directory:

```
deepdive {

  # Put your variables here
  schema.variables {
  }

  # Put your extractors here
  extraction.extractors {
  }

  # Put your inference rules here
  inference.factors {
  }

  # Specify a holdout fraction
  calibration.holdout_fraction: 0.00

}
```

In this template, the global section `deepdive` contains following major sections: `db`, `schema`, `extraction`, `inference`, `calibration`. Other optional sections are `sampler` and `execution`.

Links to these sections:

- [extraction](#extraction): extraction tasks
- [inference](#inference-opt): inference rules
- [schema](#inference-schema): variable schema
- [calibration](#calibration): calibration parameters
- [sampler](#sampler): sampler arguments


## Notation format

DeepDive configuration file uses HOCON format. It is an extension of JSON. For a detailed specification, see [readme of HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md#readme).

Below are some highlights of the notation format.

### Blocks

Blocks are specified by `{}` rather than indentation. Blocks can be nested.

Note that the following nested block definition are equivalent:

    schema {
      variables {
        ...
      }
    }

and

    schema.variables {
      ...
    }

This is often useful in making the code more compact.

### Comments

Any text appearing after a `#` or `//` and before the next new line is considered a comment, unless the `#` or `//` is inside a quoted string.

### Key-value separators

Both `:` and `=` are valid key-value separators.

## <a name="extraction" href="#"></a> Extraction and extractors

Configuration directives for executing extractors go in the
`extraction` section, while extractor definitions go in the `extraction.extractors` section:

```
deepdive {
  extraction {
    # extraction directives
  }
  extraction.extractors {
    # extractor definitions
  }
  # ...
}
```

### <a name="extractor" href="#"></a> Extractors definition

Each extractor definition is a section named with the name of the extractor:

```
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

Different styles of extractors are defined using different
sets of directives. There is nevertheless a subset of directives that are common
to all styles:

- `style`: specifies the style of the extractor. Can take the values
  `tsv_extractor`, `sql_extractor`, or
  `cmd_extractor`. This is a mandatory
  directive.

- `before`: specifies a shell command to run **before** executing the extractor.
  This is an optional directive.

    ```
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

    ```
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
  required order. E.g.:

    ```
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

- `input_relations`: takes an array of relation names that this extractor depends on.  Similar to `dependencies`, all extractors whose `output_relation` exists in this array will be executed before this extractor.

The following directives are only for the `tsv_extractor` styles.
They are **mandatory** for these styles.

- `input`: specifies the input to the extractor. For all the extractor
  styles above it can be a SQL query to run on the database, e.g.,:

    ```
    myExtractor {
      # ...
      style: "tsv_extractor"
      # ...
      input: """SELECT * FROM titles"""
      # ...
    }
    ```

- `output_relation`: specifies the name of the relation the extractor output
  should be written to. Must be an existing relation in the database. E.g.:

    ```
    myExtractor {
      # ...
      style: "tsv_extractor"
      # ...
      output_relation: words
      # ...
    }
    ```

- `udf`: specifies the extractor User Defined Function (UDF). This is a shell
  command or a script that is executed.

- Depending on the extractor style, additional directives may be necessary, such as `sql`, `cmd`, `input_batch_size`, and `output_batch_size`.


## <a name="inference-opt" href="#"></a> Inference

*Note:* this section presents configuration directive for the inference step.
Refer to the [appropriate section](#inference_rules) for the directives to define
inference rules.

Configuration directives to control the inference steps go in the global
`deepdive` section. The available directives are:

- <a name="batch_size" href="#"></a> `inference.batch_size`: batch size to
  insert variables, factors, and weights in the database during the factor graph
  creation:

    ```
    inference.batch_size = 1000000
    ```

    The default value depends on the used datastore (50000 for PostgreSQL).

- <a name="parallelgrounding" href="#"></a> `inference.parallel_grounding`. If
  set to `true` and you are using [GreenPlum
  on DeepDive](using-greenplum.md), use [parallelism](using-greenplum.md#parallelgrounding) when
  grounding the graph</a>. Default is `false`.

    ```
    inference.parallel_grounding: true
    ```

- <a name="skip_learning" href="#"></a> `inference.skip_learning`: if `true`,
  DeepDive will skip the weight learning step, and reuse the
  weights **learned in the last execution**. It will generate a table
  `dd_graph_last_weights` containing all the weights.  Weights will be matched
  by their "text description" (which is composed by `[name of inference rule]-[specified value of "weight" in inference rule]`, e.g. `myRule-male`), and no learning will be performed. To get meaningful results, A DeepDive run must be already
  performed in the database, and the view `dd_inference_result_weights_mapping`
  must be present.


    ```
    inference.skip_learning: true
    ```
    By default this directive is `false`.

- <a name="weight_table" href="#"></a> `inference.weight_table`: to be used in
  combination with `inference.skip_learning`, it allows to skip the weight
  learning step and use the weights specified in a custom table. The table
  tuples must contain the factor description and weights. Note that it is
  important that this table is constracted with the same syntax as described above.


    This table can be the result from one execution of DeepDive (an example would
    be the view `dd_inference_result_weights_mapping`, or
    `dd_graph_last_weights` used when `inference.skip_learning` is `true`) or
    manually assigned, or a combination of the two.

    If weight for a specific factor is not in the weight table, the
    weight will be treated as 0. For example, if
    `f_has_spouse_features-SOME_NEW_FEATURE` is not found in
    the specified weight table, but this factor is found in the
    inference step, the weight of it will be treated as 0.

    If `inference_skip_learning` is `false` (default) this directive is ignored.

    ```
    inference.skip_learning: true
    inference.weight_table: [weight table name]
    ```

## <a name="inference_schema" href="#"></a> Inference schema

[Inference schema](writing-model-ddlog.md#variable-relations) directives define the variables used in the
factor graph and their type. Inference schema directives go in the
`schema.variables` section:

```
deepdive {
  # ...
  schema.variables {
    # Variable definitions
  }
  # ...
}
```

A variable in DeepDive is defined by its name (table.column) and its type:

```
person_smokes.smokes: Boolean
person_has_cancer.has_cancer: Boolean
```

A table can have up to one column declared as a DeepDive variable.
This restriction makes the semantics clear such that each tuple in the database corresponds to one random variable.
DeepDive currently supports Boolean and Categorical variables.


## <a name="inference_rules" href="#"></a> Inference rules

*Note:* refer to ['Writing inference rules' document](writing-model-ddlog.md) for
an in-depth discussion about writing inference rules.

The definitions of inference rules for the factor graphs go in the
`inference.factors` section:

```
deepdive {
  inference.factors {
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
  factor. Refer to the [source code](../compiler/compile-config/compile-config-1.01-parse_inference_rules) for details about the available
  functions. Example usage:

- `weight`: specifies whether the weight of the factor should be a specified
  constant or learned (and if so, whether it should be a function of some
  columns in the input query. Possible values for this directive are:

  - a real number: the weight is the given number and not learned.

  - `"?"`: DeepDive learns a weight for all factors defined by this rule. All the
    factors will share the same weight.

  - `"?(column_name)"`: DeepDive learns multiple weights, one for each different
    value in the column `column_name` in the result of `input_query`.

An example inference rule is the following:

```
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
  weight: 0.5
}
```


## <a name="calibration" href="#"></a> Calibration / Holdout

Directive for [calibration](calibration.md) go to the `calibration` section.
The available directives are:

- `holdout_fraction`: specifies the fraction of training data to use for
  [holdout](calibration.md#holdout). E.g.:

    ```
    calibration {
      holdout_fraction: 0.25
    }
    ```
- `holdout_query`: specifies a custom query to be used to define the holdout set.
  This must insert all variable IDs that are to be held out into the
  `dd_graph_variables_holdout` table through arbitrary SQL. E.g.:

    ```
    calibration {
      holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT dd_id FROM dd_variables_mytable WHERE predicate"
    }
    ```

  When a custom holdout query is defined in `holdout_query`, the
  `holdout_fraction` setting is ignored.

- `observation_query`: specifies a custom query to be used to define observation only evidence. Observation only evidence will not be fitted during weight learning. So there will be 3 kinds of variables during learning -- evidence that will be fitted, evidence that will not be fitted and non-evidence variables. This query must insert all variable IDs that are observation only evidence into the `dd_graph_variables_observation` table through arbitrary SQL. E.g.:

    ```
    calibration {
      observation_query: "INSERT INTO dd_graph_variables_observation SELECT id FROM mytable WHERE predicate"
    }
    ```



## <a name="sampler" href="#"></a> Sampler
Configuration directives for the sampler go in the global `deepdive` section.
The available directive are:

- (Optional) `sampler.sampler_cmd`: the path to the sampler executable:

    ```
    sampler.sampler_cmd: "util/sampler-dw-mac gibbs"
    ```

    Since [version 0.03](changelog/0.03-alpha.md), DeepDive automatically
    chooses the correct executable based on your operating system (between
    `"util/sampler-dw-linux gibbs"` and `"util/sampler-dw-mac gibbs"`), so
    we recommend to
    omit the `sampler_cmd` directive.

- `sampler.sampler_args`: the arguments to the sampler executable:

    ```
    deepdive {
      sampler.sampler_args: "-l 1000 -i 1000 --alpha 0.01"
    }
    ```
    The default `sampler_args` are: `"-l 300 -i 500 --alpha 0.1"`.

    For a list and the meaning of the arguments, please refer to the
    [documentation of our DimmWitted sampler](sampler.md).

