---
layout: default
---

# Writing inference rules

Inference rules describe how to build the [factor
graph](../general/inference.html). Each rule
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

```bash
deepdive {
  inference.factors: {
    smokesFactor {
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

    # More rules...
  }
}
```

### Factor input query

The input query of a factor returns a set of tuples. Each tuple must contain all
the variables that a factor is using, plus additional columns that are used to
learn the weight of the factor. It usually takes the form of a join query
using feature relations produced by extractors, as in the following example:

```bash
someFactor {
  input_query: """
    SELECT p1.id     AS "people.p1.id",     p2.id     AS "people.p2.id",
           p1.smokes AS "people.p1.smokes", p2.smokes AS "people.p2.smokes",
           friends.person_id AS "friends.person_id"
    FROM friends
      INNER JOIN people AS p1 ON (friends.person_id = p1.id)
      INNER JOIN people AS p2 ON (friends.friend_id = p2.id)
    """
	# ...
}
```

There are a number of caveats when writing input queries for factors:

- The query result **must** contain all variable attributes that are used in your
  factor function. For example, if you are using the `people.has_cancer`
  variable in the factor function, then an attribute called `people.has_cancer`
  must be part of the query result. 

- Always use `[relation_name].[attribute]` to refer to the variables in the
factor function, regardless whether or not you are using an alias in your input
query. For example, even if you write `SELECT p1.is_male from people p1`, the
variable would be called `people.is_male`.

- The query result **must** contain the reserved column `id` for each variable.
  DeepDive uses `id` column to assign unique variable ids. For example, if you
  have `people.has_cancer` variable in your factor function, then `people.id`
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
SELECT p1.name, p1.id, p2.name, p2.id FROM people p1 LEFT JOIN people p2 ON p1.manager_id = p2.id
    ```

    Instead, you must write something like the following:

    ```sql
SELECT p1.name AS "p1.name", p1.id AS "p1.id", p2.name AS "p2.name", p2.id
AS "p2.id" FROM people p1 LEFT JOIN people p2 ON p1.manager_id = p2.id;
    ```
	Your factor function variables would be called `people.p1.name` and
	`people.p2.name`.

### Factor function

The factor function defines which variables should be connected to the factor,
and how they are related. All variables used in a factor function must have been
previously defined in the [schema](schema.html).

DeepDive supports [several types of factor
functions](inference_rule_functions.html). One example of a factor function is
the `Imply` function, which expresses a first-order logic statement. For
example, `Imply(B, C, A)` means "if B and C, then A".

```bash
    # If people.smokes, then people.has_cancer
    someFactor {
      function: "Imply(people.smokes, people.has_cancer)"
    }
    
    # Evaluates to true, when people.has_cancer is true
    someFactor {
      function: "Imply(people.has_cancer)"
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
[training data](../general/relation_extraction.html) available. The weight can
also be a function of variables, in which case each factor will get a different
weight depending on the variable value.

```bash
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
reference](configuration.html#skip_learning) for more details about this option.

#### Custom weight table

You can specify a table for the factor weights by setting
`inference.weight_table` along with `inference.skip_learning` (learning will be
skeep). This is useful to learn the weights once and use the model for later
inference tasks. Refer to the [Configuration
reference](configuration.html#weight_table) for more details about this option
and the schema of the weight table. -->

<!-- TODO (MR) All that follows must go somewhere else

### Evidence and Query variables

Evidence is training data that is used to automatically learn [factor
weights](inference_rules.html). DeepDive will treat variables with existing
values as evidence. In the above example, rows in the *people* table with a
`true` or `false` value in the *smokes* or *has_cancer* column will be treated
as evidence for that variable. Cells without a value (NULL) value will be
treated as query variables.

The inference results are stored in the database, in the table named `[variable
name]_inference`. DeepDive gives expectation for each variable, which is the
most probable value that the variable may take. Also, the learned weights are
stored in the table `dd_inference_result_weights`.
-->

