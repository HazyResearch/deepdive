---
layout: default
title: Specifying a statistical model in DDlog
---

# Specifying a statistical model in DDlog

Every DeepDive application can be viewed as defining a [statistical inference](inference.md) problem using every bits of [its input data](ops-data.md#organizing-input-data) and [data derived by a series of data processing steps](ops-execution.md#compiled-processes-and-data-flow).
This document describes how to declare random variables for a DeepDive application's statistical model, how to define their scope as well as supervision labels, and how to write inference rules for specifying features and correlations.


## Variable declarations

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
For instance, in the [chunking example](example-chunking.md), a categorical variable of 13 possible categories is declared as follows:

```ddlog
tag?(word_id bigint) Categorical(13).
```




## Scoping and Supervision rules

After declaring a variable relation, its *scope* needs to be defined along with the *supervision labels*.
That means, (a) all possible values for the variable relation's columns must be defined by deriving them from other relations, and (b) whether a random variable in the relation is true or false (Boolean), or which value it takes from its domain of categories (Categorical) must be defined using a special syntax.
For these scoping and supervision rules, a syntax very similar to normal derivation rules is used for defining the variable relation, except that the head is followed by an `=` sign and an expression that corresponds to the random variable's label.
For instance, in the spouse example, we scope the `has_spouse` variable by the following rule:

```ddlog
has_spouse(p1_id, p2_id) = NULL :-
    spouse_candidate(p1_id, _, p2_id, _).
```

This means all distinct `p1_id` and `p2_id` pairs found in the `spouse_candidate` relation is considered the scope of all `has_spouse` random variables in the model.
By using a `NULL` expression on the right hand side, they are considered as unsupervised variables.

On the other hand, the following similar looking rule provides the supervision labels using a sophisticated expression.

```ddlog
has_spouse(p1_id, p2_id) = if l > 0 then TRUE
                      else if l < 0 then FALSE
                      else NULL end :- spouse_label_resolved(p1_id, p2_id, l).
```

This rule is basically doing a *majority vote*, turning aggregate numbers computed from `spouse_label_resolved` relation into Boolean labels.





## Inference rules

*Inference rules* specify features for a variable and/or the correlations between variables.
They are basically the templates for the factors in the [factor graph](inference.md), telling DeepDive how to *ground* them based on what input and derived data.
Again, these rules extend the syntax for normal derivation rules and allows the type of the factor to be specified in the rules head, preceded by a `@weight` declaration as shown below.

```ddlog
@weight(...)
FACTOR_HEAD :- RULE_BODY.
```

Here, `RULE_BODY` is the typical conjunctive query also used for normal derivation rules in DDlog.
The `FACTOR_HEAD` is the part where more than one variable relations can appear with a special syntax.
Let's first look at the simplest case of describing one variable in the inference rule's head.


### Specifying features

In common cases, one wants to model a Boolean variable's probability of being true using some set of features.
Expressing this kind of binary classification problem is very simple in DDlog.
By writing a rule with just one variable relation in the head, DeepDive creates in the model a *unary factor* that connects to it whose weight is determined by a user-defined feature.
For instance, in [the spouse example](example-spouse.md), there is an inference rule specifying features for the `has_spouse` variables written as:

```ddlog
@weight(f)
has_spouse(p1_id, p2_id) :-
    spouse_candidate(p1_id, _, p2_id, _),
    spouse_feature(p1_id, p2_id, f).
```

This rule means that:

- A factor should be created for each pair of person mentions found in the `spouse_candidate` relation and each of the corresponding features found in `spouse_feature` relation.
- Each of those factors connects to a single `has_spouse` variable identified by a pair of mentions `(p1_id, p2_id)` originating from the `spouse_candidate` relation.
- The feature `f` for a factor determines a *weight* (to be learned for this particular rule) that translates into the factor's potential, which in turn influences the probability of the connected `has_spouse` variable being true or not.


### Specifying correlations

Now, in almost every problem, the variables are correlated with each other in a special way, and it is desirable to enrich the model with this domain knowledge.
This is done by creating certain types of factors that connect to those correlated variables, and this is where the special syntax comes into play.
DDlog syntax borrows heavily from Markov Logic Networks and first-order logic.

For example, the following rule in [the smoke example](example-smoke.md) correlates two variable relations.

```ddlog
@weight(3)
smoke(x) => cancer(x) :-
    person(x).
```

This rule expresses that if a person smokes, there's an *implication* that he/she will have cancer.
Here, a constant `3` is used in the `@weight` to express some level of confidence in this rule, instead of learning the weight from the data (explained more later).

#### Implication

[Logical implication or consequence](http://en.wikipedia.org/wiki/Truth_table#Logical_implication) of two or more variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) => Q(y)             :- RULE_BODY.
@weight(...)  P(x), Q(y) => R(z)       :- RULE_BODY.
@weight(...)  P(x), Q(y), R(z) => S(k) :- RULE_BODY.
```

#### Disjunction

[Logical disjunction](http://en.wikipedia.org/wiki/Truth_table#Logical_disjunction) of two or more variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) v Q(y)        :- RULE_BODY.
@weight(...)  P(x) v Q(y) v R(z) :- RULE_BODY.
```

#### Conjunction

[Logical conjunction](http://en.wikipedia.org/wiki/Truth_table#Logical_disjunction) of two or more variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) ^ Q(y)        :- RULE_BODY.
@weight(...)  P(x) ^ Q(y) ^ R(z) :- RULE_BODY.
```

#### Equality

[Logical equality](http://en.wikipedia.org/wiki/Truth_table#Logical_equality) of two variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) = Q(y) :- RULE_BODY.
```

#### Negation

Whenever a correlation is expressing a Boolean variable to be false (also referred to as a *negated literal*), then it can be negated using a preceding `!` as shown below.

```ddlog
@weight(...)    P(x) => ! Q(x) :- RULE_BODY.
@weight(...)  ! P(x) v  ! Q(x) :- RULE_BODY.
```

----
----
----

<todo>finish below</todo>

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

We include a typical usage example of multinomial variables in the chunking example under `examples/chunking`.
A walkthrough this example, detailing how to specify Conditional Random Fields and perform Multi-class Logistic Regression is available [here](example-chunking.md).


### Specifying weights

The **factor weight** describes the confidence in the relationship expressed by the factor.
This is used during probabilistic inference.
Weights can be constants, or automatically learned based on training data.

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




<!-- TODO
#### Using arrays in factor functions

To use array of variables in factor function, in the input query, generate corresponding variable ids in array form, and rename it as `relation.id`, where `relation` is the table containing these variables, i.e., the naming convention for array variables is same as single variables, whereas the only difference is variable ids are in array form.

-->
