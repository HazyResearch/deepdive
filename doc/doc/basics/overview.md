---
layout: default
---

# DeepDive system overview

This document presents an overview of DeepDive as a system. It assumes that you
are familiar with some general concepts like [inference and factor
graphs](../general/inference.html), [relation
extraction](../general/relation_extraction.html), and [distant
supervision](../general/distant_supervision.html). It describes each step
performed during the execution of a DeepDive application:

- [Extraction](#extraction)

- [Factor graph grounding](#grounding)
  
- [Weight learning](#weight)

- [Inference](#inference)

## <a name="extraction" href="#"></a> Extraction

The extraction step is a **data transformation** during which DeepDive processes
the data to extract [entities](../general/relation_extraction.html#entity), perform
entity linking, feature extraction, [distant
supervision](../general/distant_supervision.html), and any other task
necessary to create the variables on which it will then perform
[inference](../general/inference.html), and, if needed, to generate the training
data used for learning the factor weights. The tasks to perform during
extraction are specified by defining [extractors](extractors.html), which are
user-defined functions (UDFs). The results of extraction are stored in the
application database and will be then used to build the factor graph according
to [rules specified by the user](inference_rules.html).

## <a name="grounding" href="#"></a> Factor graph grounding

DeepDive uses a [factor graph](../general/inference.html) to perform
inference. The user writes SQL queries to instruct the system about
which variables to create. These queries usually involve tables populated during
the extraction step. The variable nodes of the factor graph are connected to
factors according to [inference rules](inference_rules.html) specified by the
user, who also defines the factor functions which describe how the variables are
related. The user can specify whether the factor weights should be constant or
learned by the system (refer to the ['Writing inference rules'
document](inference_rules.html) ). 

Grounding is the process of writing the graph to disk so that it can be used to
perform inference. DeepDive writes the graph to a set of five files: one for
variables, one for factors, one for edges, one for weights, and one for metadata
useful to the system. The format of these file is special so that they can be
accepted as input by our [sampler](sampler.html).

## <a name="weight" href="#"></a> Weight learning

DeepDive can learn the weights of the factor graph from training data that can
be either obtained through [distant
supervision](../general/distant_supervision.html) or specified by the user while
populating the database during the extraction phase. The main general way for 
learning the weights is maximum likelihood.


The learned weights are then written to a specific database table so that the
user can inspect them during the [calibration](calibration.html) of the process.

## <a name="inference" href="#"></a> Inference

The final step consists in performing [marginal
inference](../general/inference.html#marginal) on the factor graph variables to
learn the probabilities of different values they can take over all [possible
worlds](../general/inference.html#possibleworlds). DeepDive uses our
[high-throughput DimmWitted sampler](sampler.html) to perform [Gibbs
sampling](../general/inference.html#gibbs), i.e., to go through many possible
worlds and to estimate the probabilities. The sampler takes the grounded
graph (i.e., the five files written during the [grounding](#grounding) step) as
input, together with a number of arguments to specify the parameters for the
learning procedure. The results of the inference step are written to the
database. The user can write queries to [analyze the
results](running.html#results). DeepDive also provides [calibration
data](calibration.html) to evaluate the accuracy of the inference.

<!-- TODO (All) Anything else we should add ? -->

