---
layout: homepage
root: "."
---

### What is DeepDive?

DeepDive is a new type of system that enables developers to analyze data on a
deeper level than ever before. DeepDive is a **trained system**: it uses machine
learning techniques to leverage on domain-specific knowledge and incorporates
user feedback to improve the quality of its analysis.

DeepDive differs from traditional systems in several ways:

- DeepDive asks the developer to **think about features—not algorithms**.
  In contrast, other machine learning systems require the developer
  think about which clustering algorithm classification algorithm, etc.
  In DeepDive’s joint inference based approach, the user only specifies
  the necessary signals or features.
- DeepDive has achieved **winning performance** in 
  [text competitions](http://i.stanford.edu/hazy/papers/2014kbp-systemdescription.pdf) 
  and even **beat human volunteers** in extracting complex knowledge in 
  [scientific domains](http://www.plosone.org/article/info:doi/10.1371/journal.pone.0113523).
- DeepDive is aware that **data is often noisy and imprecise**: names are
  misspelled, natural language is ambiguous, and humans make mistakes. Taking
  such imprecisions into account, DeepDive computes
  [calibrated](doc/basics/calibration.html) probabilities for every assertion
  it makes. For example, if DeepDive produces a fact with probability 0.9 it
  means the fact is 90% likely to be true. 
- DeepDive is able to use large amounts of data from a **variety of sources**.
  Applications built using DeepDive have extracted data from millions of
  documents, web pages, PDFs, tables, and figures.
- DeepDive allows developers to **use their knowledge of a given domain** to
  improve the quality of the results by [writing simple
  rules](doc/basics/inference_rules.html) that inform the inference (learning) process.
  DeepDive can also take into account user feedback on the correctness of the
  predictions, with the goal of improving the predictions.
- DeepDive is able to use the data to [learn
  "distantly"](doc/general/distant_supervision.html). In contrast, most machine
  learning systems require tedious training for each prediction. In fact,
  many DeepDive applications, especially at early stages, need no traditional
  training data at all!
- DeepDive’s secret is a **scalable, high-performance inference and learning
  engine**. For the past few years, we have been working to make the underlying
  algorithms run as fast as possible. The techniques pioneered in this project
  are part of commercial and open source tools including
  [MADlib](http://madlib.net/),
  [Impala](http://www.cloudera.com/content/cloudera/en/products-and-services/cdh/impala.html),
  a product from
  [Oracle](https://blogs.oracle.com/R/entry/low_rank_matrix_factorization_in),
  and low-level techniques, such as
  [Hogwild!](http://i.stanford.edu/hazy/papers/hogwild-nips.pdf). They
  have also been included in [Microsoft's
  Adam](http://www.wired.com/2014/07/microsoft-adam/).

For more details, check out [our papers](doc/papers.html).

### Who should use DeepDive?

DeepDive is targeted to help user extract relations between entities from data
and make inference about facts involving the entities. DeepDive can process
structured, unstructured, clean, or noisy data and outputs the results into a
database.

Users should be familiar with SQL and Python in order to build applications on
top of DeepDive or to integrate DeepDive with other tools. A developer who
would like to modify and improve DeepDive must have some basic background
knowledge listed in the documentation below. 

### What is DeepDive used for?

Over the last few years, we have built applications for both broad domains that
[read the Web](https://www.youtube.com/watch?v=Q1IpE9_pBu4) and for specific
domains like paleobiology. In collaboration with Shanan Peters
([PaleobioDB](http://paleobiodb.org/)), we built a
[system](https://www.youtube.com/watch?v=Cj2-dQ2nwoY) that reads documents with
higher accuracy and from larger corpora than expert human volunteers. We find
this very exciting as it demonstrates that trained systems may have the ability
to change the way science is conducted. 

In a number of research papers we demonstrated the power of DeepDive on NMR data
and financial, oil, and gas documents. For example, we
[showed](http://cs.stanford.edu/people/chrismre/papers/jointable-acl.pdf) that
DeepDive can understand tabular data. We are using DeepDive to support our own
research, exploring how knowledge can be used to build the next generation of data
processing systems.

Examples of DeepDive applications include:

- [PaleoDeepDive](https://www.youtube.com/watch?v=Cj2-dQ2nwoY) - A knowledge base for Paleobiologists
- [GeoDeepDive](https://www.youtube.com/watch?v=X8uhs28O3eA) - Extracting dark data from geology journal articles
- [Wisci](https://www.youtube.com/watch?v=Q1IpE9_pBu4) - Enriching Wikipedia with structured data

The complete code for these examples is available with DeepDive. We are
currently working on other domains with even more collaborators. Stay tuned, and
[get in touch with us](mailto:contact.hazy@gmail.com) to talk about interesting
projects.

### Who develops DeepDive?

DeepDive is project led by [Christopher
Ré](http://cs.stanford.edu/people/chrismre/) at Stanford University. Current
group members include: [Michael Cafarella](http://web.eecs.umich.edu/~michjc/),
Amir Abbas Sadeghian, [Zifei Shan](http://www.zifeishan.org/), 
Jaeo Shin, Feiran Wang, [Sen Wu](http://stanford.edu/~senwu/), and [Ce
Zhang](http://pages.cs.wisc.edu/~czhang/).

### Updates &amp; Changelog 

- [Changelog for version 0.04.2-alpha](doc/changelog/0.04.2-alpha.html) (12/23/2014)
- [Changelog for version 0.04.1-alpha](doc/changelog/0.04.1-alpha.html) (11/25/2014)
- [Changelog for version 0.04-alpha](doc/changelog/0.04-alpha.html) (11/19/2014)
- [Changelog for version 0.03.2-alpha](doc/changelog/0.03.2-alpha.html) (09/16/2014)
- [Changelog for version 0.03.1-alpha](doc/changelog/0.03.1-alpha.html) (08/15/2014)
- [Changelog for version 0.03-alpha](doc/changelog/0.03-alpha.html) (05/07/2014)
- [Changelog for version 0.02-alpha](doc/changelog/0.02-alpha.html) (03/12/2014)

### <a name="documentation" href="#"></a> Documentation

#### Background

- [Knowledge base construction](doc/general/kbc.html)
- [Relation extraction](doc/general/relation_extraction.html)
- [Distant supervision](doc/general/distant_supervision.html)
- [Probabilistic inference and factor graphs](doc/general/inference.html)

#### Basics

- [System overview and fundamental terminology](doc/basics/overview.html)
- [Installation guide](doc/basics/installation.html)
- [Creating a new application](doc/basics/writing.html)
- [Writing extractors](doc/basics/extractors.html)
- [Declaring inference variables in the schema](doc/basics/schema.html)
- [Writing inference rules](doc/basics/inference_rules.html)
- [Inference rule function reference](doc/basics/inference_rule_functions.html)
- [Generating negative examples](doc/basics/generating_negative_examples.html)
- [Running an application](doc/basics/running.html)
- [Calibration](doc/basics/calibration.html)
- [Labeling data products](doc/basics/labeling.html)
- [Text chunking application example](doc/basics/chunking.html)
- [Generic features library](doc/basics/gen_feats.html)
- [High-speed sampler](doc/basics/sampler.html)
- [**application.conf** Reference](doc/basics/configuration.html)
- [FAQ](doc/basics/faq.html)

#### Tutorial

- [Example application walk-through](doc/basics/walkthrough/walkthrough.html)
- [Improving the results](doc/basics/walkthrough/walkthrough-improve.html)
- [Extras](doc/basics/walkthrough/walkthrough-extras.html)

#### Advanced topics

- [Performance tuning](doc/advanced/performance.html)
- [Scala developer guide for DeepDive](doc/advanced/developer.html)
- [Using DeepDive with GreenPlum](doc/advanced/greenplum.html)
- [Using DeepDive with MySQL / MySQL Cluster](doc/advanced/mysql.html)
- [Using DeepDive on Ubuntu](doc/advanced/ubuntu.html)
- [Tuffy and Markov Logic Networks (MLN)](doc/advanced/markov_logic_network.html)
- [Amazon EC2 AMI guide](doc/advanced/ec2.html)
- [Internal database schema](doc/advanced/reserved_tables.html)
- [Factor graph grounding output reference](doc/advanced/factor_graph_schema.html)

### Support

We gratefully acknowledge the support of [our sponsors](doc/support.html).
