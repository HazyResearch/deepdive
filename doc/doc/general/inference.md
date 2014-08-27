---
layout: default
---

# Probabilistic Inference and Factor Graphs

This documents presents a high-level overview of **probabilistic inference** and
an introduction to **factor graphs**, a model used by DeepDive to perform
probabilistic inference.

**Probabilistic inference** is the task of deriving the probability for one or
more random variables to take a specific value or set of values. For example, a
Bernoulli (Boolean) random variable may describe the event that John has cancer.
Such a variable could take a value of 1 (John has cancer) or 0 (John does not
have cancer). DeepDive uses probabilistic inference to estimate the probability
that the random variable takes value 1: a probability of 0.78 would mean that
John is 78% likely to have cancer.

## Factor graphs

A **factor graph** is a type of probabilistic graphical model. A factor graph
has two types of nodes:

- <a name="variables" href="#"></a> **Variables**, which can be either *evidence
  variables* when their value is known, or *query variables* when their value
  should be predicted. 

- **Factors** define the relationships between variables in the graph. Each factor
  can be connected to many variables and comes with a **factor function** to
  define the relationship between these variables. For example, if a factor
  node is connected to two variables nodes `A` and `B`, a possible factor
  function could be `imply(A,B)`, meaning that if the random variable `A` takes
  value `1`, then so must the random variable `B`. Each *factor function* has a
  *weight* associated with it, which describes how much influence the factor has
  on its variables in relative terms. In other words, the weight encodes the
  confidence we have in the relationship expressed by the factor function. If
  the weight is high and positive, we are very confident in the function that
  the factor encodes, if the weight is high and negative, we are confident that
  the function is incorrect. The weight can be learned from training data, or
  assigned manually.

<a name="possibleworlds" href="#"></a>
A **possible world** is an assignment to every variable in a factor graph. The
possible worlds are not usually equiprobable, but rather each possible world has
a different probability. The probability of a possible world is proportional to
a weighted combination of all factor functions in the graph, evaluated at the
assignments specified by the possible world. The weights can be assigned
statically or learned automatically.  In the latter case, some *training data*
is needed. Training data define a set of possible worlds and, intuitively, the
learning process chooses the weights by maximizing the probabilities of these
possible worlds.

<!-- TODO (Later) What algorithm do we use for weight learning? -->

<a name="marginal" href="#"></a>
**Marginal inference** is the task of inferring the probability of one variable
taking a particular value. Using the [law of total
probability](http://en.wikipedia.org/wiki/Law_of_total_probability), it is
straightforward to express this probability as the sum of the probabilities of
possible worlds that contain the requested value for that variable. 

<a name="gibbs" href="#"></a>
Exact inference is an intractable problem on factor graphs, but a commonly used
method for inference is **Gibbs sampling**: the process starts from a random
possible world and iterates over each variable `v`, computing a new value for it
according to a probability computed by taking into account the factor functions
of the factors that `v` is connected to and the values of the variables
connected to such factors (this is known as the *Markov blanket* of `v`), then
the process moves to a different variable and iterates. After enough iterations
over the random variables, we can compute the number of iterations during which
each variable had a specific value and use the ratio between this quantity and
the total number of iterations as an estimate of the probability of the variable
taking that value.

<!-- TODO (All) The following section doesn't seem to fit here. What shall we do
with it? -->

## Inference in DeepDive

DeepDive allows the user to write [inference
rules](../basics/inference_rules.html) to specify how to create the factor
graph. A rule expresses concepts like "If John smokes then he is likely to
have cancer" and, in other words, describes the factor function of a factor and
which variables are connected to this factor. Each rule has a *weight* (either
computed by DeepDive or assigned by the user), which represents the confidence
in the correctness of the rule. If a rule has a high positive *weight*, then the
variables appearing in the rule are likely to take on values that would make the
rule evaluate to true. In the above example, if the rule "If John smokes then he
is likely to have cancer" has a high weight and we are **sure** that John
smokes, then we are also reasonably confident that John has cancer. However, if
we are not sure whether or not John smokes, then we can not be sure about him
having cancer either. In the latter case, both "John does not have cancer," and
"John has cancer" would make the
rule evaluate to true.

This is a subtle but very important point. Contrary to many traditional machine
learning algorithms, which often assume that prior knowledge is exact and make
predictions in isolation, DeepDive performs *joint inference*: it determines the
values of all events at the same time. This allows events to influence each
other if they are (directly or indirectly) connected through inference rules.
Thus, the uncertainty of one event (John smoking) may influence the uncertainty
of another event (John having cancer). As the relationships among events become
more complex this model becomes very powerful. For example,
one could imagine the event "John smokes" being influenced by whether or not
John has friends who smoke. This is particularly useful when dealing with
inherently noisy signals, such as human language.

## Additional resources

We refer the reader interested in additional details to other good resources:

- [A Simple Factor Graph Tutorial]({{site.baseurl}}/assets/factor_graph.pdf)
- [An Introduction to Conditional Random Fields for Relational
  Learning](http://people.cs.umass.edu/~mccallum/papers/crf-tutorial.pdf)
- [PGM class on Coursera](https://www.coursera.org/course/pgm)
- [Graphical Models Lecture at
  CMU](http://alex.smola.org/teaching/cmu2013-10-701x/pgm.html)
- [Towards High-Throughput Gibbs Sampling at Scale: A Study across Storage
  Managers](http://cs.stanford.edu/people/chrismre/papers/elementary_sigmod.pdf)
- [Factor Graphs and the Sum-Product
  Algorithm](http://www.comm.utoronto.ca/~frank/papers/KFL01.pdf)
- [Scalable Probabilistic Databases with Factor Graphs and
  MCMC](http://arxiv.org/pdf/1005.1934v1.pdf)

