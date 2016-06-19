---
layout: default
title: Specifying a statistical model in DDlog
---

# Specifying a statistical model in DDlog

Every DeepDive application can be viewed as defining a [statistical inference](inference.md) problem using [input data](ops-data.md#organizing-input-data) and [data derived by a series of data processing steps](ops-execution.md#compiled-processes-and-data-flow).
This document describes (a) how to declare random variables for a DeepDive application's statistical model, (b) how to define their scope as well as supervision labels, and (c) how to write inference rules for specifying features and correlations.


## Variable declarations

DeepDive requires the user to specify the name and type of the *variable relations* that hold random variables used during probabilistic inference.
Currently DeepDive supports Boolean (i.e., Bernoulli) variables and Categorical variables.
Variable relations are declared in `app.ddlog` with a small twist to the syntax used for [declaring normal relations](writing-dataflow-ddlog.md#schema-declarations).

### Boolean variables

A question mark after the relation name indicates that it is a variable relation containing random variables rather than a normal relation used for loading or processing data to be later used by the model.
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




## Scoping and supervision rules

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
Again, these rules extend the syntax of normal derivation rules and allow the type of the factor to be specified in the rule's head, preceded by a `@weight` declaration as shown below.

```ddlog
@weight(...)
FACTOR_HEAD :- RULE_BODY.
```

Here, *RULE_BODY* denotes a typical conjunctive query also used for normal derivation rules in DDlog.
*FACTOR_HEAD* denotes the part where more than one variable relations can appear with a special syntax.
Let's first look at the simplest case of describing one variable in the head of an inference rule.


### Specifying features

In common cases, one wants to model the probability of a Boolean variable being true using a set of features.
Expressing this kind of binary classification problem is very simple in DDlog.
By writing a rule with just one variable relation in the head, DeepDive creates in the model a *unary factor* that connects to it. The weight of this unary factor is determined by a user-defined feature.
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
Such correlations can be modeled by creating certain types of factors that connect multiple correlated variables together. This is where a richer syntax in the *FACTOR_HEAD* comes into play.
DDlog borrows a lot of syntax from [Markov Logic Networks](https://en.wikipedia.org/wiki/Markov_logic_network), and hence, [first-order logic](https://en.wikipedia.org/wiki/First-order_logic).

For example, the following rule in [the smoke example](example-smoke.md) correlates two variable relations.

```ddlog
@weight(3)
smoke(x) => cancer(x) :-
    person(x).
```

This rule expresses that if a person smokes, there is an *implication* that he/she will have cancer.
Here, a constant `3` is used in the `@weight` to express some level of confidence in this rule, instead of learning the weight from the data (explained later).

#### Implication

[Logical implication or consequence](http://en.wikipedia.org/wiki/Truth_table#Logical_implication) of two or more variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) => Q(y)              :- RULE_BODY.
@weight(...)  P(x), Q(y) => R(z)        :- RULE_BODY.
@weight(...)  P(x), Q(y), R(z) => S(k)  :- RULE_BODY.
```

#### Disjunction

[Logical disjunction](http://en.wikipedia.org/wiki/Truth_table#Logical_disjunction_.28OR.29) of two or more variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) v Q(y)         :- RULE_BODY.
@weight(...)  P(x) v Q(y) v R(z)  :- RULE_BODY.
```

#### Conjunction

[Logical conjunction](http://en.wikipedia.org/wiki/Truth_table#Logical_conjunction_.28AND.29) of two or more variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) ^ Q(y)         :- RULE_BODY.
@weight(...)  P(x) ^ Q(y) ^ R(z)  :- RULE_BODY.
```

#### Equality

[Logical equality](http://en.wikipedia.org/wiki/Truth_table#Logical_equality) of two variables can be expressed using the following syntax.

```ddlog
@weight(...)  P(x) = Q(y)  :- RULE_BODY.
```

#### Negation

Whenever a rule has to refer to a case when the Boolean variable is false (also referred to as a [*negative literal*](https://en.wikipedia.org/wiki/Literal_(mathematical_logic))), then it can be negated using a preceding `!` as shown below.

```ddlog
@weight(...)    P(x) => ! Q(x)  :- RULE_BODY.
@weight(...)  ! P(x) v  ! Q(x)  :- RULE_BODY.
```

#### Multinomial factors (STALE. NEEDS REVAMP.)

DeepDive has limited support for expressing correlations of categorical variables.
The introduced syntax above can be used only for expressing correlations between Boolean variables.
For categorical variables, DeepDive only allows the conjunction of the variables each taking a certain category value being true to be expressed using a special syntax shown below:


```ddlog
@weight(...)  Multinomial( P(x), Q(y) )        :- RULE_BODY.
@weight(...)  Multinomial( P(x), Q(y), R(z) )  :- RULE_BODY.
```

`Multinomial` takes only categorical variables as arguments<sup>*</sup>, and it can be thought as a compact representation of an equivalent model with Boolean variables corresponding to each category connected by a conjunction factor for every combination of category assignments.

For example, suppose `a` is a variable taking values 0, 1, 2, and `b` is a variable taking values 0, 1.
Then, `Multinomial(a, b)` is equivalent to having factors between `a` and `b` that correspond to the following indicator functions.

* I{`a` = 0, `b` = 0}
* I{`a` = 0, `b` = 1}
* I{`a` = 1, `b` = 0}
* I{`a` = 1, `b` = 1}
* I{`a` = 2, `b` = 0}
* I{`a` = 2, `b` = 1}

Note that each of the factors above has a distinct weight, i.e., one weight for each possible assignment of variables in the `Multinomial` factor.
For more detail on how to specify Conditional Random Fields and perform Multi-class Logistic Regression using categorical factor, see [the chunking example](example-chunking.md).

<sup>*</sup> Because of this limitation, categorical variables and categorical factor support is likely to go away in a near future release, in favor of a more flexible way to express multi-class predictions and mutual exclusions.
<todo> Emulate categorical variables with functional dependencies in the Boolean variable columns, i.e., `tag?(@key word_id BIGINT, pos TEXT)` rather than `tag?(word_id BIGINT) Categorical(N)`, and replace `Multinomial` factor with conjunction of those Boolean variables with columns having functional dependencies.</todo>

### Specifying weights

Each factor is assigned a *weight*, which represents the confidence in the correlation it expresses in the model.
During [statistical inference](inference.md), these weights translate into their potentials that influence the probabilities of the connected variables.
Factor weights are real numbers and only the relative magnitude to each other matters.
Factors with larger weights have a greater impact on the connected variables than factors with smaller weights.
Weights can be fixed to a constant manually, or [they can be learned by DeepDive](ops-model.md#learning-the-weights) from the supervision labels at different granularity.
Weights can be parameterized by some data originating from the data (in most time referred to as *features*), in which case factors with different parameter values will use different weights.
In order to learn weights automatically, there must be [enough training data available](relation_extraction.md).

DDlog syntax for specifying weights for three different cases are shown below.

```ddlog
Q?(x TEXT).
data(x TEXT, y TEXT).

# Fixed weight (10 can be treated as positive infinite)
@weight(10)   Q(x) :- data(x, y).

# Unknown weight, to be learned from the data, but not depending on any variable.
# All factors created by this rule will have the same weight.
@weight("?")  Q(x) :- data(x, y).

# Unknown weight, each to be learned from the data per different values of y.
@weight(y)    Q(x) :- data(x, y).
```



<!-- TODO
#### Using arrays in factor functions

To use array of variables in factor function, in the input query, generate corresponding variable ids in array form, and rename it as `relation.id`, where `relation` is the table containing these variables, i.e., the naming convention for array variables is same as single variables, whereas the only difference is variable ids are in array form.

-->
