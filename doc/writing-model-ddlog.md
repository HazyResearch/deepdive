---
layout: default
title: Specifying a statistical model in DDlog
---

# Specifying a statistical model in DDlog

Every DeepDive application can be viewed as defining a [statistical inference](inference.md) problem using every bits of [its input data](ops-data.md#organizing-input-data) and [data derived by a series of data processing steps](ops-execution.md#compiled-processes-and-data-flow).

    This document describes how to declare random variables on which DeepDive performs inference, how to label these variables and how to write inference rules.

    First, declare variable relations on which DeepDive predicts the marginal probability.


## Variable relation declarations

DeepDive requires the user to specify the name and type of the *variable relations* that hold random variables on which to perform inference.
Currently DeepDive supports Boolean (i.e., Bernoulli) variables and Categorical variables.
Variable relations are declared in `app.ddlog` with a small twist to the syntax used for [declaring normal relations](writing-dataflow-ddlog.md#schema-declarations).

### Boolean variables

A question mark after the relation name indicates that it is a variable relation containing random variables in the model rather than a normal relation that is one step removed from the model.
The columns of the variable relation serve as a key.
The following is an example declaration of a relation of Boolean variables.

```ddlog
has_spouse?(p1_id text, p2_id text).
```

This declares a variable relation named `has_spouse` where each unique pair of `(p1_id, p2_id)` represents a different random variable in the model.


### Categorical variables

DeepDive supports categorical variables, which take integer values ranging from 0 to a user-specified upper bound.
The variable relation is declared similarly as a Boolean variable except that the declaration is followed by a `Categorical(N)` where `N` is the number of categories the variables can take, defining the size of the domain.
Each variable can take values from 0, 1, ..., `N`-1.
For instance, in the [chunking example](example-chunking.md), the categorical variable declared is as follows:

```ddlog
tag?(word_id bigint) Categorical(13).
```

<!--
TODO
The factor function for multinomial is `Multinomial`.
It takes multinomial variables as arguments, and is equivalent to having indicator functions for each combination of variable assignments.
See this [wiki page](https://github.com/HazyResearch/ddlog/wiki/DDlog-Language-Features) for more details.
-->



## Scoping and Supervision rules

After declaring a variable relation, its *scope* needs to be defined along with the *supervision labels*.
That means, (a) all possible values for the variable relation's columns must be defined by deriving them from other relations, and (b) whether a random variable in the relation is true or false (Boolean), or which value it takes from its domain of categories (Categorical) must be defined using a special syntax.
For these scoping and supervision rules, a syntax very similar to normal derivation rules is used for defining the variable relation, except that the head is followed by an `=` sign and an expression that corresponds to the random variable's label.
For instance, in the spouse example, we scope the `has_spouse` variable by the following rule:

```ddlog
has_spouse(p1_id, p2_id) = NULL :-
    spouse_candidate(p1_id, _, p2_id, _).
```

This means we will consider all distinct `p1_id` and `p2_id` pairs found in the `spouse_candidate` relation as the scope of all random variables in the model.
By using a `NULL` expression on the right hand side, they are considered as unsupervised variables.

On the other hand, the following similar looking rule provides the supervision labels using a sophisticated expression.

```ddlog
has_spouse(p1_id, p2_id) = if l > 0 then TRUE
                      else if l < 0 then FALSE
                      else NULL end :- spouse_label_resolved(p1_id, p2_id, l).
```

This rule is basically doing a *majority vote*, turning aggregate numbers computed from `spouse_label_resolved` relation into Boolean labels.
When more than one



## Writing inference rules

*Inference rules* specify features for a variable and/or the correlations between variables.
They are basically the templates for the factors in the [factor graph](inference.md), telling DeepDive how to *ground* it based on what input and derived data.
Again, these rules allow a special syntax for specifying the type of factor in the rules head preceded by a `@weight` declaration as shown below.

```ddlog
@weight(...)
FACTOR_HEAD :- RULE_BODY.
```


### Factor input query

The input query of a factor returns a set of tuples. Each tuple must contain all
the variables that a factor is using, plus additional columns that are used to
learn the weight of the factor. It usually takes the form of a join query
using feature relations produced by extractors, as in the following example:

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

<!--
#### Using arrays in factor functions

To use array of variables in factor function, in the input query, generate corresponding variable ids in array form, and rename it as `relation.id`, where `relation` is the table containing these variables, i.e., the naming convention for array variables is same as single variables, whereas the only difference is variable ids are in array form.

-->

### Factor Weights

- The **factor weight** describes the confidence in the relationship expressed
  by the factor. This is used during probabilistic inference. Weights can be
  constants, or automatically learned based on training data.

Each factor is assigned a *weight*, which expresses the confidence in the relationship it express.
In the probabilistic inference steps, factors with large weights have a greater impact on variables than factors with small weights.
Factor weights are real numbers, and are relative to each other.
You can assign factor weights manually, or you can let DeepDive learn weights automatically.
In order to learn weights automatically, you must have enough [training data](relation_extraction.md) available.
The weight can also be a function of variables, in which case each factor will get a different weight depending on the variable value.

```hocon
# Known weight (10 can be treated as positive infinite)
someFactor.weight: 10

# Learn the weight, not depending on any variables. All factors created by this rule will have the same weight.
someFactor.weight: ?

# Learn the weight. Each factor will get a different weight depending on the value of people.gender
someFactor.weight: ?(people.gender)
```

The annotation `@weight(expression)` expresses the weight of the inference rule, which can be defined by a a feature or by a constant, such that in the _imply_ inference rule in the smoke example:

```ddlog
@weight(3)
smoke(x) => cancer(x) :- person(x).
```

This rule expresses that, if a person smokes, it implies he/she has a cancer. The constant in the `@(weight)` annotation means that the rule's factors all tie to the same weight (here defined by the value 3).



### Factor function

The factor function defines which variables should be connected to the factor, and how they are related.
All variables used in a factor function must have been previously [defined in the schema](#variable-relations).

DeepDive supports [several types of factor functions](inference_rule_functions.md).
One example of a factor function is the `Imply` function, which expresses a first-order logic statement.
For example, `Imply(B, C, A)` means "if B and C, then A".
inference_rule_functions.md#ddlog-syntax

Supported DeepDive factor function and corresponding syntax are:

```ddlog
Imply  : A1, A2, ... => An
Equal  : A1 = A2 = ... = An
And    : A1 ^ A2 ^ ... ^ An
Or     : A1 v A2 v ... v An
IsTrue : A
```

where _As_ are variable relations.



#### Multinomial factors

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

We include a typical usage example of multinomial variables in the chunking example under `examples/chunking`.
A walkthrough this example, detailing how to specify Conditional Random Fields and perform Multi-class Logistic Regression is available [here](example-chunking.md).
