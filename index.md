---
layout: default
root: "."
---

### What is DeepDive all about?

DeepDive is a new type of system that enables developers to analyze data on a deeper level than ever before. DeepDive is a **trained system**, which means that it uses machine learning techniques to incorporate domain-specific knowledge and user feedback to improve the quality of its analysis. DeepDive is different from traditional systems in several ways:

- DeepDive is aware that useful **data is often noisy and imprecise**: Names are misspelled, natural language is ambiguous, and humans make errors. To help deal with such imprecision, DeepDive produces [calibrated](doc/general/calibration.html) probabilities for every assertion it makes. For example, if DeepDive produces a fact with probability 0.9 it means the fact is 90% likely to be correct. 
- DeepDive is able to use large amounts of data from a **variety of sources**. Applications built using DeepDive have extracted data from millions of documents, web pages, PDFs, tables, and figures.
- DeepDive allows developers to **use their knowledge of a given domain** to improve the quality by [writing simple rules](doc/inference_rules.html) and giving feedback on whether predictions are correct or not.
- DeepDive is able to use your data to ["distantly" learn](doc/general/relation_extraction.html). In contrast, most machine learning systems require one to tediously train each prediction. In fact, first versions of DeepDive-based systems often do not have any traditional training data at all!
- DeepDive’s secret is a **scalable, high-performance inference and learning engine**. For the past few years, we have been working to make the underlying algorithms run as fast as possible. The underlying techniques pioneered in this project are part of commercial and open source tools including [MADlib](http://madlib.net/), [Impala](http://www.cloudera.com/content/cloudera/en/products-and-services/cdh/impala.html), a product from Oracle, and low-level techniques, such as [Hogwild!](http://www.eecs.berkeley.edu/~brecht/papers/hogwildTR.pdf), have been adopted in [Google Brain](http://static.googleusercontent.com/media/research.google.com/en/us/archive/unsupervised_icml2012.pdf).


### What is DeepDive used for?

Over the last few years, we have built applications for both broad domains that [read the Web](https://www.youtube.com/watch?v=Q1IpE9_pBu4), and for specific domains, like Paleobiology. In collaboration with Shanan Peters ([http://paleobiodb.org/](http://paleobiodb.org/)), we [built a system](https://www.youtube.com/watch?v=Cj2-dQ2nwoY) that reads documents with higher accuracy and from larger corpora than expert human volunteers. This is exciting as it demonstrates that trained systems may have the ability to change the way science is conducted. 

In research papers, we have demonstrated DeepDive on financial, oil and gas documents, and NMR data. For example, we [have shown](http://cs.stanford.edu/people/chrismre/papers/jointable-acl.pdf) that DeepDive is able to understand tabular data by reading the text of the reports. We are using DeepDive to support our own research into how knowledge can be used to build the next generation of data processing systems.

A few demonstrations of DeepDive applications:

- [PaleoDeepDive - A knowledge base for Paleobiologists](https://www.youtube.com/watch?v=Cj2-dQ2nwoY)
- [GeoDeepDive - Extracting dark data from geology journal articles](https://www.youtube.com/watch?v=X8uhs28O3eA)
- [Wisci - Enriching Wikipedia with structured data](https://www.youtube.com/watch?v=Q1IpE9_pBu4)

We are posting the complete code for our examples on this site. We are working on other more domains with even more collaborators in this year. Stay tuned, and [get in touch with us](mailto:contact.hazy@gmail.com) to talk about interesting projects.

### Who are we?

DeepDive is project led by [Chris Re](http://cs.stanford.edu/people/chrismre/) at Stanford university and the University of Wisconsin-Madison. Current group members include [Ce Zhang](http://pages.cs.wisc.edu/~czhang/), [Denny Britz](http://www.linkedin.com/in/dennybritz), [Victor Bittorf](http://pages.cs.wisc.edu/~bittorf/), [Zifei Shan](http://www.zifeishan.org/), Feiran Wang, and [Mikhail Sushkov](https://www.linkedin.com/pub/mikhail-sushkov/26/638/537).

### Updates & Changelog 

- [Changelog for version 0.03-alpha (05/07/2014)](doc/changelog/0.03-alpha.html)
- [Changelog for version 0.02-alpha (03/12/2014)](doc/changelog/0.02-alpha.html)

<a id="documentation" href="#"> </a>

### Documentation

General & Background knowledge:

- [Probabilistc Inference](doc/general/inference.html)
- [Relation Extraction & Distant Supervision](doc/general/relation_extraction.html)
- [Generating negative examples (Coming soon)](doc/general/generating_negative_examples.html)
- [Using calibration data](doc/general/calibration.html)

Getting started:

- [DeepDive overview](doc/overview.html)
- [Installation guide](doc/installation.html)
- [Amazon EC2 AMI guide](doc/ec2.html)
- [Example application walkthrough](doc/walkthrough.html)

Advanced tutorials:

- [Defining schema](doc/schema.html)
- [Writing extractors](doc/extractors.html)
- [Writing inference rules](doc/inference_rules.html)
- [Inference rule function reference](doc/inference_rule_functions.html)
- [Calibration](doc/calibration.html)
- [Pipelines](doc/pipelines.html)
- [Performance tuning](doc/performance.html)
- [High-speed sampler](doc/sampler.html)
- [Internal database schema](doc/reserved_tables.html)
- [FAQ](doc/faq.html)

Learn about using various databases and distributions with DeepDive:

- [Using PostgreSQL with DeepDive](doc/postgresql.html)
- [Using GreenPlum with DeepDive](doc/greenplum.html)
- [Using DeepDive on Ubuntu](doc/ubuntu.html)

Contribute to DeepDive:

- [Developer guide](doc/developer.html)
