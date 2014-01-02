---
layout: default
---

## Writing Inference Rules

Inference rules describe how to [factor graph]() is constructed. Each rule, or *factor*, consists of three components:

- The **input query** joins together feature relations from your extractors. One factor will be created for each row in the query result.
- The **factor function** defines the variables that will be connected to each factor, and how they are related to each other.
- The **factor weight** describes how confident you are that your rule is correct, in other words, how much weight you want to put on it during probabilistic inference. Weights can be constants, or automatically learned based on training data.

### Factor Input Query

The input query of a factor combines all variables that a factor is using. It usually takes the form of a join query using feature relations produced extractors. The input query result must contain all variable attributes that are used in the factor functions.

    someFactor.input_query: "SELECT people.*, friends.* FROM people INNER JOIN friends ON people.id = friends.person_id"

### Factor Function

The factor function defines the variables that will be connected to the factor, and how they are related to each other. All variables used in a factor function must have been previously [defined in the schema](schema.html).

Currently, DeepDive only supports one type of factor function:  `Imply(...)`. The `Imply` function expresses a first-order logic statement. For example, `A = Imply(B, C)` means that, if B and C are true, then A is true.

    # If people.smokes is true, then people.has_cancer is true
    someFactor.function: "people.has_cancer = Imply(people.smokes)"
    
    # people.has_cancer is always true
    someFactor.function: "people.has_cancer = Imply()"
    
    # Variable negation is possible: If people.smokes is false, then people.has_cancer is true
    someFactor.function: "people.has_cancer = Imply(!people.smokes)" 


### Factor Weights

Each factor is assigned a *weight*, which expresses how confident you are in its rule. During probabilistic inference, factors with a large weights will have a greater impact on variables than factors with small weights. Factor weights can be any real number, and are relative to each other. You can assign factor weights manually, or you can let DeepDive learn weights automatically. In order to learn weights automatically, you must have enough [training data](schema.html) available. A weight can also be a function of variables, in which case each factor instance will get a different weight depending on the variable value.

    # Known weight
    wordsExtractor.factor.weight: 100
    
    # Learn the weight, not depending on any variales. All factors created by this rule will have the same weight.
    wordsExtractor.factor.weight: ?
    
    # Learn the weight. Each factor will get a different weight depending on the value of people.gender
    wordsExtractor.factor.weight: ?(people.gender)


### Full Example

    deepdive.inference.factors: {
      smokesFactor.input_query: "SELECT people.* FROM people"
      smokesFactor.function: "people.has_cancer = Imply(people.smokes)"
      smokesFactor.weight: "?(people.gender)"

      # More factors...
    }