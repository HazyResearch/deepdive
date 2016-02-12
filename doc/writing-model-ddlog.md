---
layout: default
title: Specifying a statistical model in DDlog
---

<todo> - check distant supervision rule - should we write all the different inference rules here or just link to the ddlog wiki page ? </todo>

Every DeepDive application can be viewed as defining a [statistical inference](inference.md) problem using every bits of [its input data](ops-data.md#organizing-input-data) and [data derived by a series of data processing steps](ops-execution.md#compiled-processes-and-data-flow).

# Specifying a statistical model in DDlog

This document describes how to [declare random variables](#variable_relations) on which DeepDive performs inference, how to [label](#labeling_variables) these variables and how to write [inference rules](#inference_rules).

First, declare variable relations on which DeepDive predicts the marginal probability.


## <a name="variable_relations" href="#"></a> Variable Relations

DeepDive requires the user to specify the name and type of the random variables on which to perform inference. Currently DeepDive supports Boolean (i.e., Bernoulli) variables and Categorical/Multinomial variables. Random variables and their types are declared in `app.ddlog`.

### Boolean variables

The following is an example of defining a schema with two Boolean variables.

```ddlog
has_spouse?(p1_id text, p2_id text).
```

A question mark after the relation name indicates that the relation is a variable relation containing random variables in the factor graph. The columns of the variable relation serve as a key of that relation. Here for instance, a variable relation `has_spouse` is defined, and each different pair of `(p1_id, p2_id)` represents a different variable.

### Categorical/Multinomial variables

DeepDive supports multinomial variables, which take integer values ranging from 0 to a user-specified upper bound. The variable relation is defined similarly as a Boolean variable where we add `Categorical(N)`, to specify that the variable domain is 0, 1, ..., N-1. For instance, in the [chunking example](example-chunking.md), the declaration in `app.ddlog` is:

```ddlog
tag?(word_id bigint) Categorical(13).
```

The factor function for multinomial is `Multinomial`. It takes multinomial variables as arguments, and is equivalent to having indicator functions for each combination of variable assignments. See this [wiki page](https://github.com/HazyResearch/ddlog/wiki/DDlog-Language-Features) for more details.



## <a name="labeling_variables" href="#"></a> Supervision rules

A supervision rule is used for scoping and supervising the random variables, i.e., defining variable set and training data. The training data is defined by linking a variable relation to a dataset for which some labels are known or defined by distant supervision. For instance, in the spouse example, we supervise the `has_spouse` variable by the following rule:

```ddlog
has_spouse(p1_id, p2_id) = if l > 0 then TRUE
                      else if l < 0 then FALSE
                      else NULL end :- spouse_label_resolved(p1_id, p2_id, l).
```

where `spouse_label_resolved` is a dataset of pairs of people for which some labels are known.


## <a name="inference_rules" href="#"></a> Writing inference rules

Inference rules describe how to build the [factor graph](inference.md).
Each rule consists of three components:

- **Variable relation(s)**.
- **Factor function**, which defines how to connect the variable relation(s) to each factor, and how these variables are related to each other.
- **Factor weight**, that describes the confidence in the relationship expressed by the factor. This is used during probabilistic inference. Weights can be constants, or automatically learned based on training data.

For instance, in the `has_spouse` example, the inference rule defining whether the `has_spouse` variable between two entities is true or not is written as:

```ddlog
@weight(f)
has_spouse(p1_id, p2_id) :-
  spouse_candidate(p1_id, _, p2_id, _),
  spouse_feature(p1_id, p2_id, f).
```

This rule means that each pair of people `(p1_id, p2_id)` in the `has_spouse` variable relation is true or not according to weights tying to the feature column in the spouse_feature relation. <todo> explain more precisely why spouse_candidates has to be used in this declaration </todo>

The annotation `@weight(expression)` expresses the weight of the inference rule, which can be defined by a a feature or by a constant, such that in the _imply_ inference rule in the smoke example:

```ddlog
@weight(3)
smoke(x) => cancer(x) :- person(x).
```

This rule expresses that, if a person smokes, it implies he/she has a cancer. The constant in the `@(weight)` annotation means that the rule's factors all tie to the same weight (here defined by the value 3).

Supported DeepDive factor function and corresponding syntax are:

```ddlog
Imply  : A1, A2, ... => An
Equal  : A1 = A2 = ... = An
And    : A1 ^ A2 ^ ... ^ An
Or     : A1 v A2 v ... v An
IsTrue : A
```

where _As_ are variable relations.















































----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----
----

<todo>merge anything missing in DDlog from below</todo>

# Writing inference rules

Inference rules describe how to build the [factor
graph](inference.md). Each rule
consists of three components:

- The **input query** specifies the variables to create. It is a SQL query that
  usually combines relations created by the extractors. **For each row** in the
  query result, the factor graph will have variables for a subset of the columns
  in that row (those specified in the factor function, see below), all
  connected by a factor. The result of the query **must** contain the reserved `id`
  column for each of the variable involved in the factor function.

- The **factor function** defines which variables (i.e., columns in the input
  query result) to connect to each factor, and how they are related to each other.

- The **factor weight** describes the confidence in the relationship expressed
  by the factor. This is used during probabilistic inference. Weights can be
  constants, or automatically learned based on training data.

The following is an example of an inference rule:

```hocon
deepdive {
  inference.factors {
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

    # More rules...
  }
}
```

### Factor input query

The input query of a factor returns a set of tuples. Each tuple must contain all
the variables that a factor is using, plus additional columns that are used to
learn the weight of the factor. It usually takes the form of a join query
using feature relations produced by extractors, as in the following example:

```hocon
    friends_smoke {
      input_query: """
          SELECT p1.id AS "person_smokes.p1.id",
                 p2.id AS "person_smokes.p2.id",
                 p1.smokes AS "person_smokes.p1.smokes",
                 p2.smokes AS "person_smokes.p2.smokes"
            FROM friends INNER JOIN person_smokes AS p1 ON
              (friends.person_id = p1.person_id) INNER JOIN person_smokes AS p2 ON
                (friends.friend_id = p2.person_id)
        """
      ...
    }
```

There are a number of caveats when writing input queries for factors:

- The query result **must** contain all variable attributes that are used in your
  factor function. For example, if you are using the `has_cancer.is_true`
  variable in the factor function, then an attribute called `has_cancer.is_true`
  must be part of the query result.

- Always use `[relation_name].[attribute]` to refer to the variables in the
factor function, regardless whether or not you are using an alias in your input
query. For example, even if you write `SELECT s1.is_true from smokes s1`, the
variable must be called `smokes.is_true`.

- The query result **must** contain the reserved column `id` for each variable.
  DeepDive uses `id` column to assign unique variable ids. For example, if you
  have `has_cancer.is_true` variable in your factor function, then `has_cancer.id`
  must also be part of the query result. There are several **requirements for
  the `id` column**:

        - When creating a table containing variables, the column `id` must be
          explicitly created, with the type of big integer, i.e. `id bigint`.

        - The value of `id` should **always** be `NULL` before the inference step.
          DeepDive fills this column with variable IDs in the inference steps, so
          any value in this column will be lost.

        - If you use Greenplum, the column `id` must not be the distribution key for
          a table.

        - If you want an unique identifier of this table to refer to, you should use
          columns other than `id`.

        - Generally, for any table in a DeepDive application, it is recommended
          **not** to use the name `id` for any column other than this reserved
          field. Meaningful column names such as `sentence_id`, `people_id` are
          recommended.

        - The values in the columns used to learn the factor weight should not be `null`.

- When using self-joins, you must avoid naming collisions by defining an alias in
your query. For example, the following will **not** work:

    ```sql
    SELECT p1.id
           p2.id
           p1.smokes
           p2.smokes
      FROM friends, person_smokes p1, person_smokes p2
      WHERE  friends.person_id = p1.person_id
        AND  friends.friend_id = p2.person_id
    ```

    Instead, you must rename the variable columns to `[table_name].[alias].[variable_name]`, like the following:

    ```sql
    SELECT p1.id AS "person_smokes.p1.id"
           p2.id AS "person_smokes.p2.id"
           p1.smokes AS "person_smokes.p1.smokes"
           p2.smokes AS "person_smokes.p1.smokes"
      FROM friends, person_smokes p1, person_smokes p2
      WHERE  friends.person_id = p1.person_id
        AND  friends.friend_id = p2.person_id
    ```

    Your factor function variables would be called `person_smokes.p1.smokes` and
    `person_smokes.p1.smokes`.

### Factor function

The factor function defines which variables should be connected to the factor,
and how they are related. All variables used in a factor function must have been
previously [defined in the schema](#variable-relations).

DeepDive supports [several types of factor
functions](inference_rule_functions.md). One example of a factor function is
the `Imply` function, which expresses a first-order logic statement. For
example, `Imply(B, C, A)` means "if B and C, then A".

```hocon
    # If smokes.is_true, then has_cancer.is_true
    someFactor {
      function: "Imply(smokes.is_true, has_cancer.is_true)"
    }

    # Evaluates to true, when has_cancer.is_true is true
    someFactor {
      function: "IsTrue(has_cancer.is_true)"
    }
```

#### Using arrays in factor functions

<!-- TODO (Amir) The following is confusing. Add an example -->

To use array of variables in factor function, in the input query, generate
corresponding variable ids in array form, and rename it as `relation.id`, where
`relation` is the table containing these variables, i.e., the naming convention
for array variables is same as single variables, whereas the only difference is
variable ids are in array form.

### Factor Weights

Each factor is assigned a *weight*, which expresses the confidence in the
relationship it express. In the probabilistic inference steps, factors with
large weights have a greater impact on variables than factors with small
weights. Factor weights are real numbers, and are relative to each other. You
can assign factor weights manually, or you can let DeepDive learn weights
automatically. In order to learn weights automatically, you must have enough
[training data](relation_extraction.md) available. The weight can
also be a function of variables, in which case each factor will get a different
weight depending on the variable value.

```hocon
# Known weight (10 can be treated as positive infinite)
someFactor.weight: 10

# Learn the weight, not depending on any variables. All factors created by this rule will have the same weight.
someFactor.weight: ?

# Learn the weight. Each factor will get a different weight depending on the value of people.gender
someFactor.weight: ?(people.gender)
```

<!-- #### Re-use learned weights
If the system already learned the weights for your factor graphs, you can tell
DeepDive to skip learning them again by setting `inference.skip_learning` in the
application configuration file. Refer to the [Configuration
reference](configuration.md#skip_learning) for more details about this option.

#### Custom weight table

You can specify a table for the factor weights by setting
`inference.weight_table` along with `inference.skip_learning` (learning will be
skeep). This is useful to learn the weights once and use the model for later
inference tasks. Refer to the [Configuration
reference](configuration.md#weight_table) for more details about this option
and the schema of the weight table. -->

<!-- TODO (MR) All that follows must go somewhere else

### Evidence and Query variables

Evidence is training data that is used to automatically learn [factor
weights](writing-model-ddlog.md). DeepDive will treat variables with existing
values as evidence. In the above example, rows in the *people* table with a
`true` or `false` value in the *smokes* or *has_cancer* column will be treated
as evidence for that variable. Cells without a value (NULL) value will be
treated as query variables.

The inference results are stored in the database, in the table named `[variable
name]_inference`. DeepDive gives expectation for each variable, which is the
most probable value that the variable may take. Also, the learned weights are
stored in the table `dd_inference_result_weights`.
-->



----
<todo>merge anything omitted in DDlog from below</todo>

# Declaring inference variables in the schema

This document describe how to declare random variables on which DeepDive
performs inference and give details about how to use [Categorical/Multinomial
variables](#multinomial).

DeepDive requires the user to specify the name and type of the random variables
on which to perform [inference](inference.md). Currently DeepDive
support Boolean (i.e., Bernoulli) variables and Categorical/Multinomial
variables. Random variables and their types are declared in the
`schema.variables` section of the `deepdive.conf` file. The following is an
example of defining the schema with two Boolean variables:

```hocon
deepdive {
  schema.variables {
    person_smokes.smokes: Boolean
    person_has_cancer.has_cancer: Boolean
  }
}
```

In the above example `person_smokes.smokes` and `person_has_cancer.has_cancer` are
Boolean variables. `person_smokes` and `person_has_cancer` are table names, and
`smokes` and `has_cancer` are boolean columns in the corresponding table. The name of variables
*must* be in the `[table].[column]` format. Each table can at most
hold one variable.

## <a name="multinomial" href="#"></a> Categorical/Multinomial variables

DeepDive supports multinomial variables, which take integer values ranging from
0 to a user-specified upper bound. In order to use a multinomial variable, you
can declare it in the `schema.variables` directive in `deepdive.conf` using
type `Categorical(N)`, to specify that the variable domain is be 0, 1, ..., N-1.
The schema definition would be  look like

```hocon
schema.variables {
  [table].[column]: Categorical(10)
}
```

The factor function for multinomial is `Multinomial`. It takes multinomial
variables as arguments, and is equivalent to having indicator functions for each
combination of variable assignments.


For examples, if `a` is a variable taking values 0, 1, 2, and `b` is a variable
taking values 0, 1. Then, `Multinomial(a, b)` is equivalent to the following
factors between a and b

    I{a = 0, b = 0}
    I{a = 0, b = 1}
    I{a = 1, b = 0}
    I{a = 1, b = 1}
    I{a = 2, b = 0}
    I{a = 2, b = 1}

Note that each of the factor above has a corresponding weight, i.e., we have one
weight for each possible assignment of variables in the multinomial factor.

### Chunking Example

We include a typical usage example of multinomial variables in the chunking
example under `examples/chunking`. A walkthrough this example, detailing how to
specify Conditional Random Fields and perform Multi-class Logistic Regression is
available [here](example-chunking.md).

----
