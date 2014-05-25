---
layout: default
---

Example Application Walkthrough
====

<!-- TODO add table of contents with bootstrap? -->

## Introduction

Knowledge base construction (KBC) is the process of populating a knowledge base (KB) with facts (or assertions) extracted from text. 
For example,  One may want to build a medical knowledge base of interactions between drugs and disease, 
a Paleobiology knowledge base to understand when and where did dinosaurs live,
or a knowledge base of people's relationships such as spouse, parents or sibling.

As a concrete example, one may build an application to extract spouse relations from sentences in the Web. 
Figure 1 below shows such an application, where input is sentences like "U.S President Barack Obama's wife Michelle Obama ...", and DeepDive would generate a tuple in `has_spouse` table between "Barack Obama" and "Michelle Obama" with a high probability, based on [probabilistic inference](general/inference.html).

<!-- ![Data flow]({{site.baseurl}}/images/walkthrough/example_hasspouse.png) -->
<p style="text-align: center;"><img src="{{site.baseurl}}/images/walkthrough/example_hasspouse.png" alt="Data flow" style="width: 50%; text-align: center;"/>
  <br />
  <strong>Figure 1: An example KBC application</strong>
</p>

This tutorial will walk you through a common KBC pipeline and its data flow, and introduce how a KBC application is implemented in DeepDive.


## Overview of KBC in DeepDive


### Data Model

To understand the KBC data model we use in DeepDive, we first define some terms:

- An **entity** is an object in the real world, such as a person, an animal, a time period, a location, etc. For example, the person "Barack Obama" is an entity.

- **Entity-level data** are relational data over the domain of conceptual entities (as opposed to language-dependent mentions), such as relations in a knowledge base, e.g. ["Barack Obama" in Freebase](http://www.freebase.com/m/02mjmr).

- A **mention** is a reference to an entity, such as the word "Barack" in the sentence "Barack and Michelle are married". 

- **Mention-level data** are textual data with mentions of entities, such as sentences in web articles, e.g. sentences in New York Times articles.

- **Entity linking** is the process to find out which entity is a mention referring to. For example, "Barack", "Obama" or "the president" may refer to the same entity "Barack Obama", and entity linking is the process to figure this out.

- A **mention-level relation** is a relation among mentions rather than entities. For example, in a sentence "Barack and Michelle are married", the two mentions "Barack" and "Michelle" has a mention-level relation of `has_spouse`.

- An **entity-level relation** is a relation among entities. For example, the entity "Barack Obama" and "Michelle Obama" has an entity-level relation of `has_spouse`.

Above data model is demonstrated in Figure 2 below.

<!-- **TODO candidate, feature, prediction?** -->

<p style="text-align: center;"><img src="{{site.baseurl}}/images/walkthrough/datamodel.png" alt="Data Model" style="width: 60%; text-align: center;"/>
  <br />
  <strong>Figure 2: Data Model for KBC</strong>
</p>

<!-- Note that this data model is what we
use in this example walkthrough, but it isn't the only data model that
that can be used for KBC. For example, traditional distant supervision
doesn't necessarily model mention-level relations in that way (which I
think is called at-least-once semantics).
 -->

### Data Flow

Data flow of a typical KBC system in DeepDive is described below:

<!-- ![Data flow]({{site.baseurl}}/images/walkthrough/dataflow.png) -->

**The input** of the system is raw articles in text format.

**The output** of the system is a database (the knowledge base) containing our desired (entity-level or mention-level) relations.

Data will go through the following steps:

1. data preprocessing
2. feature extraction
3. factor graph generation by declarative language
4. statistical inference and learning
5. get and check results

Specifically:

1. In **data preprocessing** step, DeepDive takes input data (articles in text format), loads them into a database, and parse the articles to obtain sentence-level information including words in each sentence, POS tags, named entity tags, etc.

2. In **feature extraction** step, DeepDive converts input data into relation signals called **evidence**, by running [extractors](extractors.html) written by developers. Evidence includes: (1) candidates for (mention-level or entity-level) relations; (2) (linguistic) features for these candidates.

3. In the next step, DeepDive feeds evidence to **generate a [factor graph](general/inference.html)**. To tell DeepDive how to generate this factor graph, developers use a SQL-like declarative language to specify *inference rules*, similar to [Markov logic](http://en.wikipedia.org/wiki/Markov_logic_network). In inference rules, one can write first-order logic rules with **weights** (that intuitively model our confidence in a rule).

4. In the next step, DeepDive automatically performs **statistical inference and learning** on the generated factor graph. In learning, the values of weights specified in inference rules are calculated. In [inference](general/inference.html), marginal probabilities of variables are computed.

5. After inference, the results are stored in a set of database tables. Developer can **get results** via a SQL query, **check results** with a [calibration plot](general/calibration.html), and perform **error analysis** to improve results.

As an example of this data flow, Figure 3 demonstrates how a sentence "U.S President Barack Obama's wife Michelle Obama..." go through the process (In this figure, we only highlight step 1--3):

<p style="text-align: center;"><img src="{{site.baseurl}}/images/walkthrough/dataflow.png" alt="Data Flow" style="width: 75%; text-align: center;"/>
  <br />
  <strong>Figure 3: Data Model for KBC</strong>
</p>

In the example above:

1. During data preprocessing, the sentence is processed into words, POS tags and named entity tags; 
2. During feature extraction, DeepDive extracts (1) mentions of person and location, (2) candidate relations of `has_spouse`, and (3) `feature` of candidate relations (such as words between mentions).
3. In factor graph generation, DeepDive use rules written by developers (like `inference_rule_1` above) to build a factor graph. 
4. DeepDive will perform learning and inference.
5. Check results in a database table via SQL queries.

## A Simple Mention-Level Extraction System

To get started, let's build a system to extract **mention-level relations** first. 

**See [how to build a simple mention-level extraction system](walkthrough-mention.html).**

<a id="entity_level" href="#"></a>

## Simple Entity Level Extensions

Sorry, but we haven't finished this part yet. Stay tuned!
