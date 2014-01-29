---
layout: default
root: "."
---


### What is DeepDive all about?

DeepDive is a new type of system that enables developers to to analyze data on a deeper level than ever before. DeepDive is a **trained system**, which means that it uses machine learning techniques to incorporate domain-specific knowledge and user feedback to improve the quality of its analysis. DeepDive is different from traditional systems in several ways:

- DeepDive is aware that useful **data is often noisy and imprecise**: Names are misspelled, natural language is ambiguous, and humans make errors. To help deal with such imprecision, DeepDive produces a [calibrated](doc/calibration.html) probabilities for every assertion it makes. For example, if DeepDive produces a fact with probability 0.9 it means the fact is 90% likely to be correct. 
- DeepDive is able to use large amounts of data from a **variety of sources**. Applications built using DeepDive have extracted data from millions of documents, web pages, PDFs, tables, and figures.
- DeepDive allows developers to **use their knowledge of a given domain** to improve the quality by [writing simple rules](doc/inference_rules.html) and giving feedback on whether predictions are correct or not.
- DeepDive is able to use your data to ["distantly" learn](http://www.stanford.edu/~jurafsky/mintz.pdf). In contrast, most machine learning systems require one to tediously train each prediction. In fact, first versions of DeepDive-based systems often do not have any traditional training data at all!
- DeepDive’s secret is a **scalable, high-performance inference and learning engine**. For the past few years, we have been working to make the underlying algorithms run as fast as possible. The underlying techniques pioneered in this project are part of commercial and open source tools including [MADlib](http://madlib.net/), [Impala](http://www.cloudera.com/content/cloudera/en/products-and-services/cdh/impala.html), products from Oracle, and low-level techniques, such as [Hogwild!](http://www.eecs.berkeley.edu/~brecht/papers/hogwildTR.pdf), have been adopted in [Google Brain](http://static.googleusercontent.com/media/research.google.com/en/us/archive/unsupervised_icml2012.pdf).


### What is DeepDive used for?

Over the last few years, we have built applications for both broad domains that [read the Web](https://www.youtube.com/watch?v=Q1IpE9_pBu4), and for specific domains, like Paleobiology. In collaboration with Shanan Peters ([http://paleobiodb.org/](http://paleobiodb.org/)), we [built a system](https://www.youtube.com/watch?v=Cj2-dQ2nwoY) that reads documents with higher accuracy and from larger corpora than expert human volunteers. This is exciting as it demonstrates that trained systems may have the ability to change the way science is conducted. 

In research papers, we have demonstrated DeepDive on financial, oil and gas documents, and NMR data. For example, we [have shown](http://cs.stanford.edu/people/chrismre/papers/jointable-acl.pdf) that DeepDive is able to understand tabular data by reading the text of the reports. We are using DeepDive to support our own research into how knowledge can be used to build the next generation of data processing systems.

We are posting the complete code for our examples on this site. We are working on other more domains with even more collaborators in this year. Stay tuned, and get in [touch with us]() to talk about interesting projects.


### Download

Using the [Virtual Machine]() is the easiest way to get started with DeepDive. It comes pre-loaded with a ready-to-run example application. You can also download and compile the [DeepDive code](http://github.com/dennybritz/deepdive) yourself. For instructions on how to compile DeepDive, refer to the [developer guide](doc/developer.html).

- [Download the Virtual Machine (coming soon)]()
- [Download the source code](https://github.com/dennybritz/deepdive/archive/master.zip)



### Documentation

Getting started:

- [DeepDive overview](doc/overview.html)
- [Example application walkthrough (Coming soon)](doc/example.html)

Background knowledge:

- [Introduction to Relation Extraction](doc/general/relation_extraction.html)
- [Distant Supervision](doc/general/distant_supervision.html)
- [Generating negative examples (Coming soon)](doc/general/generating_negative_examples.html)
- [Probabilistc Inference](doc/general/inference.html)
- [Using calibration data](doc/general/calibration.html)

Learn more about DeepDive's features:

- [Defining schema](doc/schema.html)
- [Writing extractors](doc/extractors.html)
- [Writing inference rules](doc/inference_rules.html)
- [Inference rule function reference](doc/inference_rule_functions.html)
- [Calibration](doc/calibration.html)
- [Pipelines](doc/pipelines.html)
- [Performance tuning](doc/performance.html)
- [FAQ](doc/faq.html)

Learn about using various databases with DeepDive:

- [Using PostgreSQL with DeepDive](doc/postgresql.html)
- [Using GreenPlum with DeepDive](doc/greenplum.html)

Contribute to DeepDive:

- [Developer guide](doc/developer.html)
