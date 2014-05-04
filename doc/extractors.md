---
layout: default
---

# Writing Extractors

Extractors are powerful platforms provided by DeepDive for streamline feature extraction. This tutorial demonstrates how to write different kinds of extractors in DeepDive.

## Types of Extractors

DeepDive support five kinds of extractors: 

- Parallel extractors:
  - `json_extractor`: for flexibility and compatibility to previous systems
  - `tsv_extractor`: moderate flexibility and performance
  - `plpy_extractor`: parallel database-built-in extractors with restricted flexibility

- Single-threaded extractors:
  - `sql_extractor`: a SQL command
  - `cmd_extractor`: an arbitrary shell command

The first three types of extractors perform a user-defined function (UDF) on an input query against database. One may think of these extractors as functions which map one input tuple to one or more output tuples, similar to a `map` or `flatMap` function in functional programming languages. The latter two are simply SQL / shell commands.

To use different extractors, users specify `style` in each extractor definition in the configuration file. For example, if we want to use `tsv_extractor` for `wordsExtractor`:

    deepdive {
      extraction.extractors {

        wordsExtractor {
          style: "tsv_extractor"
          # ...
        }

        # More Extractors...
      }
    }

For each extractor, if `style` is not specified, the system will use `json_extractor` by default.

### json_extractor (default)

<!-- *This extractor is deprecated. Use tsv_extractor for flexibility, or plpy_extractor for performance.* -->

`json_extractor` takes data defined by an `input` query (for example, a SQL statement), and produces new tuples as output. These tuples are written to the `output_relation`. The function for this transformation is defined by the `udf` key, which can be an arbitrary executable (more on that below).

    wordsExtractor {
      style           : "json_extractor"
      output_relation : "words"
      input           : """SELECT * FROM titles"""
      udf             : "words.py"
    }

#### Writing extractor UDFs for json_extractor

When your `json_extractor` is executed, DeepDive will stream JSON tuples from the database to its *stdin*, one tuple per line. Such a tuple may look as follows:

    { title_id: 5, title: "I am a title" }

In case of reading from a CSV or TSV file, each line will be an array instead of a JSON object, for example:

    ["1", "true", "Hello World", ""]

The extractor should output JSON objects to *stdout* in the same fashion. All output tuples you must have the same fields. If you do not want to set a value for a field you can set it to `null`.


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
        "title_id": int(row["title_id"]), 
        "word": word
      })
{% endhighlight %}

#### Extractor inputs

Currently DeepDive supports two types of extractor inputs for `json_extractor`:

**1. Executing a database query**

For example, a SQL statement for Postgres:

    wordsExtractor.input: """SELECT * FROM customers"""


**2. Reading from a CSV or TSV File**

Reading a file is useful for loading initial data.

    wordsExtractor.input: CSV('path/to/file.csv')
    wordsExtractor.input: TSV('path/to/file.tsv')

Note that `sql_extractor` and `cmd_extractor` do not take inputs; `plpy_extractor` and `tsv_extractor` only takes input as a SQL query.



### tsv_extractor

`tsv_extractor` is very similar to the default `json_extractor`, but its performance is optimized: TSV instead of JSON is used for faster speed. When this type of extractors are executed in DeepDive, they go through following procedures:

1. Results of the input query will be unloaded into multiple `.tsv` files.
2. Extractor UDFs will be executed in parallel, with these files piped into STDIN.
3. Extractor outputs (also in `.tsv` format) to STDOUT will be loaded to database with a COPY command.

Same to `json_extractor`, it takes data defined by an `input` query and produces new tuples as output. These tuples are written to the `output_relation`. The function for this transformation can be any executable defined in `udf`.

    wordsExtractor {
      style           : "tsv_extractor"
      output_relation : "words"
      input           : """SELECT * FROM titles"""
      udf             : "words.py"
    }

#### Writing extractor UDFs for tsv_extractor

When your `tsv_extractor` is executed, DeepDive will stream raw lines split by `\t` from the database to its *stdin*, one row per line. Such a row may look as follows:

    "5\tI am a title"

The extractor should output JSON objects to *stdout* in the same fashion. All output tuples you must have the same fields. If you do not want to set a value for a field you can set it to `"\N"`, which will be parsed as `null` by database COPY command.


    "5\tI"
    "5\tam"
    "5\ta"
    "5\ttitle"
    "6\t\N"  # example of returning a NULL value

You can debug extractors by printing output to *stderr* instead of stdin. The output will appear in the DeepDive log file.

An extractor UDF could be written in Python as follows:

{% highlight python %}
#! /usr/bin/env python

import fileinput

# For each input row
for line in fileinput.input():
  title_id, title = line.split('\t')
  if title is not None:
    # Split the sentence by space
    for word in set(row["title"].split(" ")):
      # Output the word
      print title_id + '\t' + word
{% endhighlight %}

#### Extractor inputs

Extractor inputs for `json_extractor` must be a database query. For example, a SQL statement for Postgres:

    wordsExtractor.input: """SELECT title_id, title FROM title"""

The order of columns in the query will be the same as order in the `.tsv` file extractors get, i.e. after a line is split by `\t`, the fields are first `title_id` then `title` in this case.

#### Caveats

If your input query contains arrays,  `.tsv` files will be hard to parse.  In this case it's recommended to use `array_to_string`function to process the array in input query, then parse the string in UDF.

For example, for an input query like this:

    SELECT words_id, array_to_string(words, '$$$') FROM words_table;

You can parse each line with following:

    words_id, words_str = line.split('\t')
    words = words_str.split('$$$')
    for word in words:
      ...

Note that if your **returned value contains arrays**, it will be harder for database to parse. You should either make sure the value can be parsed by psql-COPY command, or try other types of extractors.

### plpy_extractor

`plpy_extractor` is a high-performance type of extractors for Postgresql / Greenplum databases. It avoids additional I/O by executing the extractor inside the database systems in parallel. It **translates** user's Python UDF into [PL/python](http://www.postgresql.org/docs/8.2/static/plpython.html) programs accepted by PostgreSQL databases.

Similar as `json_extractor`, the UDF is executed on data defined by an `input` SQL statement, and produces new tuples as output. These tuples are written to the `output_relation`. The function for this transformation is defined by the `udf` key, which is defined in a python script with specific format (more on that below). An example extractor is as follows:

    # An extractor to get trigrams of words from sentences to word_3gram table
    ngramExtractor {
      style           : "plpy_extractor"
      input           : """SELECT sentence_id, words, 3 FROM sentences"""
      output_relation : "word_3gram"
      udf             : "ext_word_ngram.py"
    }

Specifically, the arguments it takes include:

- style: "plpy_extractor"
- input: "*A SQL QUERY*"
- output_relation: "*TABLE NAME TO INSERT RETURN TUPLES*"
- udf: "*YOUR SCRIPT COHERENT TO PLPY_UDF FORMAT*"  (cannot take parameters)
- after / before / dependencies: See usage in section below.

#### Writing extractor UDFs for plpy_extractor

UDF in `plpy_extractor` is a python program written in a restricted framework. An example, `ext_word_ngram.py` used by above extractor `ngramExtractor` is as follows:


{% highlight python %}
#! /usr/bin/env python

import ddext

def init():
  ddext.input('sentence_id', 'bigint')
  ddext.input('words', 'text[]')
  ddext.input('gram_len', 'int')

  ddext.returns('sentence_id', 'bigint')
  ddext.returns('ngram', 'text')
  ddext.returns('count', 'int')

def run(sentence_id, words, gram_len):
  # Count Ngrams of words in the sentence
  ngram = {}
  for i in range(len(words) - gram_len):
    gram = ' '.join(words[i : i + gram_len])
    if gram not in ngram: 
      ngram[gram] = 0
    ngram[gram] += 1
    
  # All tuples have the same sentence_id, while ngram and count vary.
  return ([sentence_id], ngram.keys(), ngram.values())
{% endhighlight %}

**UDF format.** Since it will be translated into PL/Python by translator in DeepDive,  UDF of plpy_extractor must be written in a specific format:

- Must contain only two functions: `init` and `run`. 
  - Anything out of functions "init" and "run" will be ignored in the translator.

- In `init` function, import libraries, specify input variables and return types.

  1. **import libraries** by calling function `ddext.import_lib`. The function is defined as: `def import_lib(libname, from_package=None, as_name=None)`, corresponding to `from from_package import libname as as_name` in Python syntax. Sample usage:

      - `ddext.import_lib(X, Y, Z)`: from Z import X as Y
      - `ddext.import_lib(X, Y)`: from Y import X
      - `ddext.import_lib(X, as_name=Z)`: import X as Z
      - `ddext.import_lib(X)`: import X

  2. **Input variables** to UDF should be explicitly specified by `ddext.input`, both variable **names** and **types**. The function is defined as: `def input(name, datatype)`. 

      - Sample usage:

          - `ddext.input('sentence_id', 'bigint')` specifies an input to UDF with name "sentence_id" and type "bigint".

      - Caveats:

          - Names should be coherent to argument list of `run` function.

          - Types are Postgres types, e.g. `int`, `bigint`, `text`, `float`, `int[]`, `bigint[]`, `text[]`, `float[]`, etc.

          - **Orders of input matters.** `ddext.input` must be called in the same order of SQL input query. 

          - Names DO NOT need to be same as SQL input query, but types need to match.
        
  3. **Return types** should be explicitly specified by `ddext.returns`, also both variable **names** and **types**. The function is defined as: `def returns(name, datatype)`. 

      - Sample usage:
          - `ddext.returns('sentence_id', 'bigint')` specifies an output from UDF with name "sentence_id" and type "bigint".

      - Caveats:
          - Types are Postgres types as above.
          - **Names and types** of return variables should **EXACTLY MATCH** some columns in output_relation. e.g. The above example program `ext_word_ngram.py` will return tuples and call a SQL function `INSERT INTO word_3gram(sentence_id, ngram, count)`. If output relation contains more columns, those unspecified columns will be NULL in the inserted tuples.

- In `run` function, write your extractor function that will be run on all rows in your input SQL. Return **a list/tuple**.

  - **Caveats:** Use Python as you normally would, except:

    - `print` is NOT supported. If you want to print to log, use `plpy.info('SOME TEXT')` or `plpy.debug('SOME TEXT')`.
    - Do not **reassign input variables** in `run` function! 

      - e.g. "input_var = x" is invalid and will cause error!

    - Libraries imported in `init` are recognizable in `run`. 

      - e.g. `ddext.import_lib('re')` will enable you to call function `re.sub` in `run`.
      - Libraries must be already installed where your database server runs.
    
  - **Return Type:**

    - The function `run` should return a list or tuple `(list_1, list_2, ..., list_N)`, which further contains `N` lists, where `N` is the number of return variables. Each list `list_i` contains `M` rows, `list_i[j]` is the value of column `i` in `j`th row.

      - The database will try to `unnest` each column of the returned list. If the number of rows in each list matches (say all `M` rows), `M` tuples will be inserted into the database; if the numbers do not match, a cross product of these columns will be inserted. 
      - Therefore, an alternative (and faster) way to return results is to only return one element in `list_i` if the element is fixed for all results, just as the example above: `return ([sentence_id], ngram.keys(), ngram.values())`. This will make all returned tuples have the first column `sentence_id`, while other columns varies.

  - **Functions:** If you want to use functions other than `init` and `run`, you should NOT define it outside these functions. What you should do is to **define the functions inside `run`** as nested functions. An example goes follows, which nest the function `get_ngram` inside `run`:


        def run(sentence_id, words):
          ngram = {}

          # Count Ngrams of words; N as input
          # words / ngram is accessible in the function
          def get_ngram(N):
            for i in range(len(words) - N):
              gram = ' '.join(words[i : i + N])
              
              if gram not in ngram: 
                ngram[gram] = 0
              ngram[gram] += 1

          for n in range(1, 5):
            get_ngram(n)
              
          return ([sentence_id], ngram.keys(), ngram.values())


You can debug extractors by printing output using *plpy.info* or *plpy.debug* instead of *print*. The output will appear in the DeepDive log file.


### sql_extractor

To simplify users' workload, we have `sql_extractor` feature extractor which only updates the data in database(without any return results). The function framework for this extractor is defined as below.

    wordsExtractor {
      style : "sql_extractor"
      sql   : """INSERT INTO titles VALUES (1, 'Harry Potter')"""
    }

#### Extractor SQL query

For `sql_extractor`, A field `sql` is required to specify the SQL query to be executed in DeepDive.

For example, a SQL statement for Postgres:

    wordsExtractor.sql: """INSERT INTO titles VALUES (1, 'Harry Potter')"""

Note that it is developers' responsibility to run `ANALYZE table_name` after updating the table for SQL optimization.

### Cmd_extractor

The same as `sql_extractor` feature extractor, we have another extractor `cmd_extractor` which only executes a shell command. The function framework for this extractor is defined as below.

    wordsExtractor {
      style : "cmd_extractor"
      cmd   : """python words.py"""
    }

#### Execute shell command

For `cmd_extractor`, A field `cmd` is required to specify the SQL query to be executed in DeepDive.

For example, a shell command:

    wordsExtractor.cmd: """python words.py"""




----

## Other Fields in Extractors

### Extractor Dependencies

You can also specify dependencies for an extractor. Extractors will be executed in order of their dependencies. If the dependencies of several extractors are satisfied at the same time, these may be executed in parallel, or in any order.

    wordsExtractor {
      dependencies: ["anotherExtractorName"]
    }

If any extractor specified in dependencies does not exist or is not in the [pipeline](doc/pipelines.html), it will be ignored. 


### Before and After scripts

Sometimes it is useful to execute a command, or call a script, before an extractor starts or after an extractor finishes. You can specify arbitary commands to be executed as follows:

  
    wordsExtractor {
      before : """echo Hello World"""
      after  : """/path/to/my/script.sh"""
    }


### Extractor parallelism and input batch size (for json_extractor only)

To improve performance, you can specify the number of processes and the input batch size for each extractor. Your executable script will be run on N threads in parallel and data will be streamed to this processes in a round-robin fashion. By default each extractor uses 1 process and a batch size of 1000.
    
    wordsExtractor {
      # Start 5 processes for this extractor
      parallelism: 5
      # Stream 1000 tuples to each process in a round-robin fashion
      input_batch_size: 1000
    }


To improve performance when writing extracted data back to the database you can optionally specify an `output_batch_size` for each extractor. The output batch size specifies how many extracted tuples we insert into the database at once. For example, if your tuples are very large, a smaller batch size may help avoid out-of-memory errors. The default value is 10,000.

    wordsExtractor {
      # Insert each 5000 tuples into the data store
      output_batch_size: 5000
    }


You can also execute independent extractors in parallel:

<!-- TODO Shouldn't common configs be at deepdive.extraction level, e.g., deepdive.extraction.parallelism?  Otherwise, config keys may collide with user-defined extractor names. -->

    deepdive {
      extraction.extractors {
        parallelism: 5

        # Extractors...
      }
    }


