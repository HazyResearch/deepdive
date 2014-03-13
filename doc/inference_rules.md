---
layout: default
---

# Writing Inference Rules

Inference rules describe how [factor graph](/doc/general/inference.html) is constructed. Each rule, or *factor*, consists of three components:

- The **input query** usually combines relations created by your extractors. One factor will be created for each row in the query result.
- The **factor function** defines the variables that will be connected to each factor, and how they are related to each other.
- The **factor weight** describes how confident you are that your rule is correct, in other words, how much weight you want to put on it during probabilistic inference. Weights can be constants, or automatically learned based on training data.

For example:

    deepdive.inference.factors: {
      smokesFactor.input_query: """
        SELECT people.id as "people.id", people.smokes as "people.smokes", 
        people.has_cancer as "people.has_cancer" FROM people"""
      smokesFactor.function: "Imply(people.smokes, people.has_cancer)"
      smokesFactor.weight: "?(people.gender)"

      # More factors...
    }

### Factor Input Query

The input query of a factor combines all variables that a factor is using. It usually takes the form of a join query using feature relations produced extractors.

    someFactor.input_query: """SELECT p1.id AS "people.p1.id", p2.id AS "people.p2.id", 
      p1.smokes AS "people.p1.smokes", p2.smokes AS "people.p2.smokes", 
      friends.person_id AS "friends.person_id" 
      FROM friends 
        INNER JOIN people as p1 ON (friends.person_id = p1.id) 
        INNER JOIN people as p2 ON (friends.friend_id = p2.id)
    """

There are a couple of caveats when writing input queries for factors:

- The query result must contain all variable attributes that are used in your factor function. For example, if you are using `people.has_cancer` in your factor function, then an attribute called `people.has_cancer` must be part of the query result.
- The query result must contain a *unique identifier* for each relation. DeepDive uses this identifier to assign unique variable ids. For example, if you are using an RDBMS like PostgreSQL and you have a `people.has_cancer` variable in your factor function, then `people.id` must also be part of the query result. 

Refer to the database-specific guides to learn about more caveats:

- [Using DeepDive with PostgreSQL](postgresql.html) 
- [Using DeepDive with Greenplum](greenplum.html) 

### Factor Function

The factor function defines the variables that will be connected to the factor, and how they are related to each other. All variables used in a factor function must have been previously [defined in the schema](schema.html).

DeepDive supports [several types of factor functions](/doc/inference_rule_functions.html). One example of a factor function is the `Imply` function, which expresses a first-order logic statement. For example, `Imply(B, C, A)` means "if B and C, then A".

    # If people.smokes, then people.has_cancer
    someFactor.function: "Imply(people.smokes, people.has_cancer)"
    
    # Evaluates to true, when people.has_cancer is true
    someFactor.function: "Imply(people.has_cancer)"


### Factor Weights

Each factor is assigned a *weight*, which expresses how confident you are in its rule. During probabilistic inference, factors with large weights will have a greater impact on variables than factors with small weights. Factor weights can be any real number, and are relative to each other. You can assign factor weights manually, or you can let DeepDive learn weights automatically. In order to learn weights automatically, you must have enough [training data](/doc/general/relation_extraction.html) available. A weight can also be a function of variables, in which case each factor instance will get a different weight depending on the variable value.

    # Known weight
    wordsExtractor.factor.weight: 100
    
    # Learn the weight, not depending on any variales. All factors created by this rule will have the same weight.
    wordsExtractor.factor.weight: ?
    
    # Learn the weight. Each factor will get a different weight depending on the value of people.gender
    wordsExtractor.factor.weight: ?(people.gender)
