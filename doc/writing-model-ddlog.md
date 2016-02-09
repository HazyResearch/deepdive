---
layout: default
title: Specifying a statistical model in DDlog
---

<todo> - check distant supervision rule - should we write all the different inference rules here or just link to the ddlog wiki page ? </todo>

Every DeepDive application can be viewed as defining a [statistical inference](inference.md) problem using every bits of [its input data](ops-data.md#organizing-input-data) and [data derived by a series of data processing steps](ops-execution.md#compiled-processes-and-data-flow).

# Specifying a statistical model in DDlog

This document describe how to [declare random variables](#variable_relations) on which DeepDive performs inference, how to [label](#labeling_variables) these variables and how to write [inference rules](#inference_rules).

First, variable relations on which DeepDive will predict the marginal probability are to be declared.


## <a name="variable_relations" href="#"></a> Variable Relations

DeepDive requires the user to specify the name and type of the random variables on which to perform inference. Currently DeepDive support Boolean (i.e., Bernoulli) variables and Categorical/Multinomial variables. Random variables and their types are declared in `app.ddlog`.

### Boolean variable

The following is an example of defining the schema with two Boolean variables.

```
has_spouse?(p1_id text, p2_id text).
```

A question mark after the relation name indicates that the relation is a variable relation containing random variables in the factor graph. The columns of the variable relation serve as a key of that relation. Here for instance, a variable relation has\_spouse is defined, and each different pair of `(p1_id, p2_id)` represents a different variable.

### Categorical/Multinomial variables

DeepDive supports multinomial variables, which take integer values ranging from 0 to a user-specified upper bound. The variable relation is defined similarly as a boolean variable where we add `Categorical(N)`, to specify that the variable domain is 0, 1, ..., N-1. For instance, in the chunking example <todo> add link </todo>, the declaration in `app.ddlog` is:

```
tag?(word_id bigint) Categorical(13).
```

The factor function for multinomial is `Multinomial`. It takes multinomial variables as arguments, and is equivalent to having indicator functions for each combination of variable assignments. See this [wiki page](https://github.com/HazyResearch/ddlog/wiki/DDlog-Language-Features) for more details.

## <a name="labeling_variables" href="#"></a> Supervision rules

A supervision rule is used for scoping and supervising the random variables, i.e., defining variable set and training data. The training data is defined by linking a variable relation to a dataset for which some labels are known or defined by distant supervision. For instance, in the spouse example, we supervise the `has_spouse` variable by the following rule:

```
has_spouse(p1_id, p2_id) = if l > 0 then TRUE
                      else if l < 0 then FALSE
                      else NULL end :- spouse_label_resolved(p1_id, p2_id, l).
```

where `spouse_label_resolved` is a dataset of pairs of people for which some labels are known.


## <a name="inference_rules" href="#"></a> Writing inference rules

Inference rules describe how to build the [factor graph](inference.md).
Each rule consists of three components:

- The **variable relation(s)**.
- The **factor function** which defines how to connect the variable relation(s) to each factor, and how these variables are related to each other.
- the **factor weight** that describes the confidence in the relationship expressed by the factor. This is used during probabilistic inference. Weights can be constants, or automatically learned based on training data.

For instance, in the spouse example, the inference rule defining whether the `has_spouse` variable between two entities is true or not is defined by:

```
@weight(f)
has_spouse(p1_id, p2_id) :-
  spouse_candidate(p1_id, _, p2_id, _),
  spouse_feature(p1_id, p2_id, f).
```

This rule means that each pair of people `(p1_id, p2_id)` in the `has_spouse` variable relation is true or not according to weights tying to the feature column in the spouse_feature relation. <todo> explain more precisely why spouse_candidates has to be used in this declaration </todo>

The annotation `@weight(expression)` expresses the weight of the inference rule, which can be defined by a a feature or by a constant, such that in the _imply_ inference rule in the smoke example:

```
@weight(3)
smoke(x) => cancer(x) :- person(x).
```

This rule expresses that, if a person smokes, it implies he/she has a cancer. The constant in the `@(weight)` annotation means that the rule's factors all tie to the same weight (here defined by the value 3).

Supported DeepDive factor function and corresponding syntax are:

```
Imply  : A1, A2, ... => An
Equal  : A1 = A2 = ... = An
And    : A1 ^ A2 ^ ... ^ An
Or     : A1 v A2 v ... v An
IsTrue : A
```

where _As_ are variable relations.

