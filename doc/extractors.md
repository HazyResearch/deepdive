---
layout: default
---

# Writing Extractors

### Defining extractors in the configuration file

Deepdive system support three kinds of extractors: `udf_extractor`, `sql_extractor`, and `cmd_extractor`. To use different extractor, we define a keyword `style` in the extractor definition to recognize the extractor. For example, if we want to use `udf_extractor`:

	wordsExtractor.style: "udf_extractor"


### Udf_extractor
`udf_extractor` feature extractor takes data defined by an `input` (for example, a SQL statement), and produces new tuples as ouput. These tuples are written to the `output_relation`. The function for this transformation is defined by the `udf` key, which can be an arbitary executable (more on that below).

    deepdive.extractions: {
      wordsExtractor.style: "udf_extractor"
      wordsExtractor.output_relation: "words"
      wordsExtractor.input: "SELECT * FROM titles"
      wordsExtractor.udf: "words.py"
      # More Extractors...
    }


#### Extractor inputs

Currently DeepDive supports two types of extractor inputs:

**1. Reading from a CSV or TSV File**

Reading a file is useful for loading initial data.

    wordsExtractor.input: CSV('path/to/file.csv')
    wordsExtractor.input: TSV('path/to/file.tsv')

**2. Executing a database query**

For example, a SQL statement for Postgres:

    wordsExtractor.input: "SELECT * FROM customers"


#### Extractor Dependencies

You can also specify dependencies for an extractor. Extractors will be executed in order of their dependencies. If the dependencies of several extractors are satisfied at the same time, these may be executed in parallel, or in any order.

    wordsExtractor.dependencies: ["anotherExtractorName"]



#### Before and After scripts

Sometimes it is useful to execute a command, or call a script, before an extractor starts or after an extractor finishes. You can specify arbitary commands to be executed as follows:

  
    wordsExtractor.before: "echo Hello World"
    wordsExtractor.after: "/path/to/my/script.sh"


#### Extractor parallelism and input batch size

To improve performance, you can specify the number of processes and the input batch size for each extractor. Your executable script will be run on N threads in parallel and data will be streamed to this processes in a round-robin fashion. By default each extractor uses 1 process and a batch size of 1000.
    
    # Start 5 processes for this extractor
    wordsExtractor.parallelism: 5
    # Stream 1000 tuples to each process in a round-robin fashion
    wordsExtractor.input_batch_size: 1000


To improve performance when writing extracted data back to the database you can optionally specify an `output_batch_size` for each extractor. The output batch size specifies how many extracted tuples we insert into the database at once. For example, if your tuples are very large, a smaller batch size may help avoid out-of-memory errors. The default value is 10,000.

    # Insert each 5000 tuples into the data store
    wordsExtractor.output_batch_size: 5000


You can also execute independent extractors in parallel:

    deepdive.extractions: {
      parallelism: 5
      # Extractors...
    }


#### Writing extractor UDFs

When your extractor is executed, DeepDive will stream JSON tuples from the database to its *stdin*, one tuple per line. Such a tuple may look as follows:

    { id: 5, title: "I am a title" }

In case of reading from a CSV or TSV file, each line will be an array instead of a JSON object, for example:

    ["1", "true", "Hello World", ""]

The extractor should output JSON objects to *stdout* in the same fashion, but **without the `id` field**, which is automatically assigned by DeepDive. All output tuples you must have the same fields. If you do not want to set a value for a field you can set it to `null`.


    { title_id: 5, word: "I" } 
    { title_id: 5, word: "am" } 
    { title_id: 5, word: "a" } 
    { title_id: 5, word: "title" } 

You can debug extractors by printing output to *stderr* instead of stdin. The output will appear in the DeepDive log file.

An extractor UDF could be written in Python as follows:

{% highlight python %}
#! /usr/bin/env python

import fileinput
import json

# For each input row
for line in fileinput.input():
  # Load the JSON object
  row = json.loads(line)
  if row["title"] is not None:
    # Split the sentence by space
    for word in set(row["title"].split(" ")):
      # Output the word
      print json.dumps({
        "title_id": int(row["id"]), 
        "word": word
      })
{% endhighlight %}

### Sql_extractor

To simplify users' workload, we have `sql_extractor` feature extractor which only updates the data in database(without any return results). The function framework for this extractor is defined as below.

    deepdive.extractions: {
      wordsExtractor.style: "sql_extractor"
      wordsExtractor.sql: "INSERT INTO titles VALUES (1, 'Harry Potter')"
      # More Extractors...
    }

#### Extractor Sql query

For example, a SQL statement for Postgres:

      wordsExtractor.sql: "INSERT INTO titles VALUES (1, 'Harry Potter')"


#### Extractor Dependencies

You can also specify dependencies for an extractor. Extractors will be executed in order of their dependencies. If the dependencies of several extractors are satisfied at the same time, these may be executed in parallel, or in any order.

    wordsExtractor.dependencies: ["anotherExtractorName"]


### Cmd_extractor

The same as `sql_extractor` feature extractor, we have another extractor `cmd_extractor` which only executes a shell command. The function framework for this extractor is defined as below.

    deepdive.extractions: {
      wordsExtractor.style: "cmd_extractor"
      wordsExtractor.cmd: "words.py"
      # More Extractors...
    }

#### Execute shell command

For example, a shell command:

      wordsExtractor.cmd: "words.py"


#### Extractor Dependencies

The setting of specify dependencies for `cmd_extractor` extractor is the same with other extractors.

    wordsExtractor.dependencies: ["anotherExtractorName"]

