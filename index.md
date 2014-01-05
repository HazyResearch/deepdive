---
layout: default
root: "."
---

### What is DeepDive all about?

DeepDive is a new type of system that enables developers to to analyze data on a deeper level than ever before. DeepDive is a *trained* system, which means that it uses Machine Learning techniques to incorporate domain knowledge and user feedback to improve the quality of the analysis. DeepDive is differnet from traditional systems in several ways:


- Traditional systems assume that data is perfectly accurate. In contrast, DeepDive is aware that useful data is often noisy and imprecise: Names are misspelled, natural language is ambiguous, and human errors exist. To deal with such imprecision, DeepDive uses a principled approach based on probability theory. DeepDive produces calibrated probabilities for every fact or mapping it produces. For example, if DeepDive produces a fact with probability 0.9 it means it is 90% likely to be correct.
- DeepDive is able to use large amounts of data from a variety of sources. Applications built using DeepDive have extracted rich structured data from millions of documents, web pages, PDFs, tables, figures, and hundreds hours of audio data. DeepDive is able to use domain knowledge across these signal to improve the quality of the result.
- DeepDive's secret sauce is a scalable inference engine that does not cut corners on the model. We have been working like maniacs for years to eek performance out of the hardware and changes to the underlying mathematical algorithms to make them run faster. The underlying techniques pioneered in this project are part of commercial and open source tools including [MADlib](http://madlib.net/), [Impala](http://www.cloudera.com/content/cloudera/en/products-and-services/cdh/impala.html), products from Oracle, and low-level techniques are used in the Google's [Hogwild!](http://www.eecs.berkeley.edu/~brecht/papers/hogwildTR.pdf).


### What is DeepDive used for?

Over the last few years, we have built applications that [scale to the Web](https://www.youtube.com/watch?v=Q1IpE9_pBu4), and applications that read and understand Paleobiology documents. In collaboration with Shanan Peters ([http://paleobiodb.org/](http://paleobiodb.org/)), we [built a system](https://www.youtube.com/watch?v=Cj2-dQ2nwoY) that reads documents with higher accuracy, and from larger corpora, than trained human volunteers. This is exciting as it shows that trained systems have the ability to dramatically change the way science and business are conducted.

In research papers, we have demonstrated DeepDive on financial, oil and gas documents, and NMR data. For example, we [have shown](http://cs.stanford.edu/people/chrismre/papers/jointable-acl.pdf) that DeepDive is able to understand tabular data by reading the text of the reports. We are using DeepDive to support our own research into how knowledge can be used to build the next generation of data processing systems.

We are posting the complete code for our examples on this site. We are workin on other more domains with even more collaborators in this year. Stay tuned, and get in [touch with us]() to talk about interesting projects.



### Download

Using the [Virtual Machine]() is the easiest way to get started with DeepDive. It comes pre-loaded with a ready-to-run example application. You can also download and compile the [DeepDive code](http://github.com/dennybritz/deepdive) yourself. For instructions on how to compile DeepDive, refer to the [developer guide]().

- [Download the Virtual Machine]()
- [Download the source code](https://github.com/dennybritz/deepdive/archive/master.zip)

### Documentation

To get started:

- [DeepDive overview](doc/overview.html)
- [Example application walkthrough](doc/example.html)

Learn more about DeepDive's features:

- [Defining schema](doc/schema.html)
- [Writing extractors](doc/extractors.html)
- [Writing inference rules](doc/inference_rules.html)
- [Calibration](doc/calibration.html)
- [Performance tuning](doc/performance.html)

Learn about using various databases with DeepDive:

- [Using PostgreSQL with DeepDive](doc/postgresql.html)

Contribute to DeepDive:

- [Developer guide](doc/developer.html)
