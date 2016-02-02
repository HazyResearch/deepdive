---
layout: default
title: Writing user-defined functions in Python
---

# Writing user-defined functions in Python

In the previous section [Defining Data Processing in DDlog](writing-dataflow-ddlog.md) we saw that we can write UDFs for various purposes including extraction and supervision.  We saw how to define the function in ddlog and to call it in order to write it's output to a defined relation.  In this section we will look into the python files to see an example structure and see some utilities that DeepDive provides to make writing UDFs easier.  


Note that DeepDive supports running any executable but here we demonstrate an example in Python because DeepDive's ddlib provides utilities to easily parse the input rows and format the output rows.


## General Guidelines


## ddlib - @tsv_extractor, @returns, default parameters


### @tsv_extractor


### @returns


### Default parameters
