---
layout: default
title: DeepDive application development cycle
---

# How DeepDive applications are typically developed

All successful DeepDive applications that achieve high quality are never developed in a single shot.
It is common that users start with a basic first version that usually gives relatively poor results. Then, the quality is **improved iteratively** through *error analysis* and debugging.
DeepDive provides a suite of tools that aim to keep each development iteration as quick and easy as possible.


## 1. Write

First of all, the user has to write in DDlog a schema that describes the input data and the data to be produced, along with how data should be processed and transformed.
Data transformation rules as well as *user-defined functions* (UDFs) written in Python or any other language can be used for defining the data processing operations.
Then, using the processed data, a statistical inference model describing a set of random variables and their correlations can be defined&mdash;also in DDlog&mdash;to specify what kind of predictions are to be made by the system.

How to write each of these parts [in a DeepDive application](deepdiveapp.md) is described in the following pages:

- [Defining data flow in DDlog](writing-dataflow-ddlog.md)
- [Writing user-defined functions in Python](writing-udf-python.md)
- [Specifying a statistical model in DDlog](writing-model-ddlog.md)


## 2. Run

As soon as a new piece of the DeepDive application is written, the user can compile and run it incrementally.
For example, after declaring the schema for the input text corpus, the actual data can be loaded into the database and queried.
New tranformation rules added by the user can be executed incrememtally.
The statistical inference model can be constructed from the data according to the DDlog specification. The model's parameters can be learned or reused to make predictions about the marginal probabilities of new data points.

DeepDive provides a suite of commands and conventions to carry out these operations. These are documented in the following pages:

- [Compiling a DeepDive application](ops-compiling.md)
- [Managing input data and data products](ops-data.md)
- [Controlling execution of data processing](ops-execution.md)
- [Learning and inference with the statistical model](ops-model.md)


## 3. Evaluate / Debug

Based on our observation of [several successful DeepDive applications](showcase/apps.md), we can say that the more rapid the user moves through the development cycle, the more quick she achieves high-quality.
Users can identify the most common mode of errors after each iteration by evaluating the predictions made by the system. This is done via formal error analysis supported by interactive tools.
This enables the user to focus on fixing the mistakes that caused the observed errors, instead of spending her time poorly on some corners that might only give marginal improvement.

DeepDive provides a suite of tools and guides to accelerate the development process, which are documented in the following pages:

- [Debugging UDFs](debugging-udf.md)
- [Labeling data products](labeling.md)
- [Browsing data](browsing.md)
- [Monitoring descriptive statistics of data](dashboard.md)
- [Calibration](calibration.md)
- [Generating negative examples](generating_negative_examples.md)
