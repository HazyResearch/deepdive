---
layout: default
---

# DeepDive Overview

When writing applications on top of DeepDive, you often go through several phases:

1. **Data Preparation** - Set up the database and load initial data
2. **Feature Extraction** - Write *extractors* to extract features from your data
3. **Probabilistic Inference** - Write *inference rules* to help [make predictions](/doc/general/probabilistic_inference.html) on unknown variables
4. **Calibration** - Evaluate results and iterate on your feature extractors and inference rules

### 1. Data Preparation

DeepDive assumes that you have initial data stored in a relational datastore, such as [PostgreSQL](http://postgresql.org/) or [Greenplum](http://gopivotal.com/products/pivotal-greenplum-database). We hope to also support different types of SQL- and NoSQL datastores in the future. Loading initial data is not part of the DeepDive pipeline and is often done in a data preparation script. Such a script is useful to automate the workflow as much as possible. For an example of waht such a script may look like, take a look at the examples that ship with Deepdive.

### 2. Feature Extraction

[Feature Extraction](http://en.wikipedia.org/wiki/Feature_extraction) is the process of transforming raw data into a set of *features* that capture important information about the data. The goal of the feature extraction step is to extract useful features that can be used during probabilistic inference. For example, in Information Extraction applications, you may extract features such as part-of-speech tags, named-entity-tags, or dependency parses of raw text.

DeepDive provides an abstraction called *extractors* to streamline feature extraction. The input of an extractor is defined by a query against the database. DeepDive converts this data to JSON, and sends it to the extractor. Each extractor takes JSON tuples as input, and produces JSON tuples as output. One may think of an extractor as a function whichs maps one input tuple to one or more output tuples, similar to a `map` or `flatMap` function in functional programming languages. The output of an extractor is written back to the data store by DeepDive, and can be used in other extractors and/or during the inference step.

Because extractors operate on *stdin* and *stdout* they can be arbitary executable files, written in any programming language. If you are familiar with Hadoop, this is quite similiar to how Hadoop streaming functions are written. For example, one could write an extractor that extracts words form a sentence in Python as follows:

{% highlight python %}
#! /usr/bin/env python

import fileinput
import json

# For each input row
for line in fileinput.input():
  # Load the JSON object
  row = json.loads(line)
  if row["titles.title"] is not None:
    # Split the sentence by space
    for word in set(row["titles.title"].split(" ")):
      # Output the word
      print json.dumps({
        "title_id": int(row["titles.id"]), 
        "word": word
      })
{% endhighlight %}


### 3. Probabilistic Inference

The goal of inference is to make predictions on a set of *variables*. For example, you may want to predict whether or not a person has cancer based on a number of symptoms (features).[For a high-level overview of probabilistic inference, please read the corresponding guide](/doc/general/probabilistic_inference.html).

DeepDive uses [factor graphs](http://en.wikipedia.org/wiki/Factor_graph), a type of graphical model, to perform probabilistic inference. On a high-level, a factor graph has two types of nodes:

- *Variables*, which are called *evidence variables* when their value is known, or *query varibles* when their value should be predicted. *Evidence variables* can be used as training data to learn weights for factors.
- *Factors* define how variables in the graph are related to each other. Each factor can be connected to many variables, and uses a *factor function* to define the relationship between these variables. Each *factor function* has a *weight* associated with it, which describes how much influence the factor has on its variables in relative terms. The weight can be learned from training data, or assigned manually.

While you don't need to be initimately familiar with factor graphs to be able to use DeepDive, it is a good idea to have a basic understanding of how they work. A few good resources can be found here:


- [Factor Graphs and the Sum-Product Algorithm](http://www.comm.utoronto.ca/~frank/papers/KFL01.pdf)
- [Towards High-Throughput Gibbs Sampling at Scale: A Study across Storage Managers](http://cs.stanford.edu/people/chrismre/papers/elementary_sigmod.pdf)
- [Scalable Probabilistic Databases with Factor Graphs and MCMC](http://arxiv.org/pdf/1005.1934v1.pdf)
- [Graphical Models Lecture at CMU](http://alex.smola.org/teaching/cmu2013-10-701x/pgm.html)
- [PGM class on Coursera](https://www.coursera.org/course/pgm)

DeepDive exposes a language to easily build factor graphs by writing *rules* that define the relationships between varibles. 

    smokesFactor.input_query: "SELECT * from people"
    smokesFactor.function: "Imply(people.smokes, people.has_cancer)"
    smokesFactor.weight: ?

For example, the rule above states that if a person smokes, he or she is likely to have cancer, and that the weight of the rule should be learned automatically based on training data. You can use DeepDive's language to express complex relationships that use features which you have previously extracted.


### 4. Calibration

Writing factors and rules is an iterative process. You may find that a certain rule does not work as well as expected, or that you need to extract more features. DeepDive wants to make this process as easy as possible, so it writes *calibration data*, which helps you to figure out where the bottlenecks in your system are. [Take a look at the calibration overview for more details](doc/general/calibration.html).

![]({{site.baseurl}}/images/calibration_example.png)




