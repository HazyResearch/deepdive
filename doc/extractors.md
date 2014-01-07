---
layout: default
---

# Writing Extractors

### Defining extractors in the configuration file

A feature extractor takes data defined by an `input` (for example, a SQL statement), and produces new tuples as ouput. These tuples are written to the `output_relation`. The function for this transformation is defined by the `udf` key, which can be an arbitary executable (more on that below).

    deepdive.extractions: {
      wordsExtractor.output_relation: "words"
      wordsExtractor.input: "SELECT * FROM titles"
      wordsExtractor.udf: "words.py"
      # More Extractors...
    }


### Extractor inputs

Currently DeepDive supports two types of extractor inputs:

**1. Reading from a CSV or TSV File**

Reading a file is useful to load initial data.

    wordsExtractor.input: CSV('path/to/file.csv')
    wordsExtractor.input: TSV('path/to/file.tsv')

**2. Executing a database query**

For example, a SQL statement for Postgres:

    wordsExtractor.input: "SELECT * FROM customers"



### Extractor Dependencies

You can also specify dependencies for an extractor. Extractors will be executed in order of their dependencies. If the dependencies of several extractors ar satisfied at the same time, these may be executed in parallel, or in any order.

    wordsExtractor.dependencies: ["anotherExtractorName"]


### Extractor parallelism and input batch size

To improve performance, you can specify the number of processes and the input batch size for each extractor. Your executable script will be run on N threads in parallel and data will be streamed to this processes in a round-robin fashion. By default each extractor uses 1 process and a batch size of 1000.
    
    # Start 5 processes for this extractor
    wordsExtractor.parallelism: 5
    # Stream 1000 tuples to each process in a round-robin fashion
    wordsExtractor.input_batch_size: 1000


To improve performance when writing extracted data back to the database you can optionally specify an `output_batch_size` for each extractor. The output batch size specifies how many extracted tuples we insert into the database at once. For example, if your tuples are very large, a smaller batch size may help avoid out-of-memory errors. The default value is 10,000.

    # Insert each 5000 tuples into the data store
    wordsExtractor.output_batch_size: 5000


### Writing extractor UDFs

When your extractor is executed, DeepDive will stream JSON tuples to its *stdin*, one tuple per line. Such a tuple may look as follows:

    { id: 5, title: "I am a title" }

The extractor should output JSON tuples to *stdout* in the same way, but **without the `id` field**, which is automatically assigned by DeepDive. All output tuples you must have the same fields. If you do not want to set a value for a field you can set it to `null`.


    { title_id: 5, word: "I" } 
    { title_id: 5, word: "am" } 
    { title_id: 5, word: "a" } 
    { title_id: 5, word: "title" } 

An extractor UDF could be written in Python as follows:

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
