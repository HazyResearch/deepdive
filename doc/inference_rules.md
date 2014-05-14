---
layout: default
---

# Writing Inference Rules

Inference rules describe how [factor graph](general/inference.html) is constructed. Each rule, or *factor*, consists of three components:

- The **input query** usually combines relations created by your extractors. One factor will be created for each row in the query result.
- The **factor function** defines the variables that will be connected to each factor, and how they are related to each other.
- The **factor weight** describes how confident you are that your rule is correct, in other words, how much weight you want to put on it during probabilistic inference. Weights can be constants, or automatically learned based on training data. 

For example:

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

        # More factors...
      }
    }

### Factor Input Query

The input query of a factor combines all variables that a factor is using. It usually takes the form of a join query using feature relations produced extractors.

    someFactor {
      input_query: """
        SELECT p1.id     AS "people.p1.id",     p2.id     AS "people.p2.id",
               p1.smokes AS "people.p1.smokes", p2.smokes AS "people.p2.smokes",
               friends.person_id AS "friends.person_id"
        FROM friends
          INNER JOIN people AS p1 ON (friends.person_id = p1.id)
          INNER JOIN people AS p2 ON (friends.friend_id = p2.id)
        """
    }

There are a couple of caveats when writing input queries for factors:

- The query result must contain all variable attributes that are used in your factor function. For example, if you are using `people.has_cancer` in your factor function, then an attribute called `people.has_cancer` must be part of the query result.
- The query result must contain the reserved column `id` for each variable. DeepDive uses `id` column to assign unique variable ids. For example, if you have `people.has_cancer` variable in your factor function, then `people.id` must also be part of the query result. **Several requirements for `id` column**:
  - When creating a table containing variables, the column `id` must be explicitly created, with the type of big integer, i.e. `id bigint`.
  - The value of `id` should be `NULL` in all phases before inference. System will fill this column with variable IDs during inference, and values in this column will be lost.
  - If using Greenplum, `id` must not be the distribution key for a table.
  - If developers want an unique identifier of this table to refer to, they should use other columns rather than `id`.
  - Generally, for any table in a DeepDive application, name `id` is NOT recommended to use for any column other than this reserved field. Meaningful column names such as `sentence_id`, `people_id` are recommended.

Refer to the database-specific guides to learn about more caveats:

- [Using DeepDive with PostgreSQL](postgresql.html) 
- [Using DeepDive with Greenplum](greenplum.html) 

### Factor Function

The factor function defines the variables that will be connected to the factor, and how they are related to each other. All variables used in a factor function must have been previously [defined in the schema](schema.html).

DeepDive supports [several types of factor functions](inference_rule_functions.html). One example of a factor function is the `Imply` function, which expresses a first-order logic statement. For example, `Imply(B, C, A)` means "if B and C, then A".

    # If people.smokes, then people.has_cancer
    someFactor {
      function: "Imply(people.smokes, people.has_cancer)"
    }
    
    # Evaluates to true, when people.has_cancer is true
    someFactor {
      function: "Imply(people.has_cancer)"
    }


### Factor Weights

Each factor is assigned a *weight*, which expresses how confident you are in its rule. During probabilistic inference, factors with large weights will have a greater impact on variables than factors with small weights. Factor weights can be any real number, and are relative to each other. You can assign factor weights manually, or you can let DeepDive learn weights automatically. In order to learn weights automatically, you must have enough [training data](general/relation_extraction.html) available. A weight can also be a function of variables, in which case each factor instance will get a different weight depending on the variable value.

    # Known weight (10 can be treated as positive infinite)
    someFactor.weight: 10
    
    # Learn the weight, not depending on any variales. All factors created by this rule will have the same weight.
    someFactor.weight: ?
    
    # Learn the weight. Each factor will get a different weight depending on the value of people.gender
    someFactor.weight: ?(people.gender)


#### Use Learned Weights

To rerun the pipeline but use weights learned in the last execution instead of learning again, set `inference.skip_learning` to `true`. This will generate a table `dd_graph_last_weights` containing all the weights. Weights will be matched by description, and no learning will be performed:

    deepdive {
      # Use weights learned from last execution
      inference.skip_learning: true
    }


#### Custom Weight Table

Set `inference.weight_table` along with `inference.skip_learning` to fix factor weights in a table and skip learning. The table is specified by factor description and weights. This table can be results from one execution of DeepDive (an example would be the view `dd_inference_result_variable_mapped_weights`, or `dd_graph_last_weights` mentioned above), or manually assigned, or a combination of them. It is useful for learning once and using learned model for later inference tasks:

    deepdive {
      # Use weights in [weight table name]
      inference.skip_learning: true
      inference.weight_table: [weight table name]
    }

