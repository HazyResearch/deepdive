---
layout: default
title: Writing user-defined functions in Python
---

# Writing user-defined functions in Python

DeepDive supports *user-defined functions* (UDFs) for data processing, in addition to the [normal derivation rules in DDlog](writing-dataflow-ddlog.md).
UDF can be any program that takes TAB-separated JSONs (TSJ) format or TAB-separated values ([TSV or PostgreSQL's text format](http://www.postgresql.org/docs/9.1/static/sql-copy.html#AEN64351)) from stdin and prints the same format to stdout.
TSJ puts a fixed number of JSON values in a fixed order in each line, separated by a TAB.
TSJ can be thought as a more efficient encoding than simply putting a JSON object per line, which has to repeat the field names on every line, especially when the fixed data schema for every line is known and fixed ahead of time.

The following sections describe DeepDive's recommended way of writing UDFs in Python and how they are used in DDlog programs.


## Using UDFs in DDlog

To use user-defined functions in DDlog, they must be declared first then called using special syntax.

First, let's define the schema of the two relations for our running example.

```ddlog
article(
    id     int,
    url    text,
    title  text,
    author text,
    words  text[]
).

classification(
    article_id int,
    topic      text
).
```

In this example, let's suppose we want to write a simple UDF to classify each `article` into different topics by adding tuples to the relation `classification`. The two following sections detail how to declare such a function and how to call it in DDlog.

### Function declarations
A function declaration includes input/output schema as well as a pointer to its implementation.

```ddlog
function <function_name> over (<input_var_name> <input_var_type>,...)
    returns [(<output_var_name> <output_var_type>,...) | rows like <relation_name>]
    implementation "<executable_path>" handles tsj lines.
```

In our example, suppose we will use only the `author` and `words` of each `article` to determine the topic identified by its `id`, and the implementation will be kept in an executable file with relative path `udf/classify.py`.
The exact declaration for such function is shown below.

```ddlog
function classify_articles over (id int, author text, words text[])
    returns (article_id int, topic text)
    implementation "udf/classify.py" handles tsj lines.
```

Notice that the column definitions of relation `classification` are repeated in the `returns` clause.
This can be omitted by using the `rows like` syntax as shown below.

```ddlog
function classify_articles over (id int, author text, words text[])
    return rows like classification
    implementation "udf/classify.py" handles tsj lines.
```

Also note that the function input is similar to the `articles` relation, but some columns are missing.
This is because the function will not use the rest of the columns as mentioned before, and it is a good idea to drop unnecessary values for efficiency.
Next section shows how such input tuples can be derived and fed into a function.


### Function call rules
The function declared above can be called to derive tuples for another relation of the output type.
The input tuples for the function call are derived using a syntax similar to a [normal derivation rule](writing-dataflow-ddlog.md#normal-derivation-rules).
For example, the rule shown below calls the `classify_articles` function to fill the `classification` relation using a subset of columns from the `articles` relation.

```ddlog
classification += classify_articles(id, author, words) :-
    article(id, _, _, author, words).
```

Function call rules can be viewed as special cases of normal derivation rules with different head syntax, where `+=` and a function name is appended to the head relation name.

## Writing UDFs in Python

DeepDive provides a templated way to write user-defined functions in Python.
It provides several [Python function decorators](https://www.python.org/dev/peps/pep-0318/) to simplify parsing and formatting input and output respectively.
The [Python generator](https://www.python.org/dev/peps/pep-0255/) to be called upon every input row should be decorated with `@tsj_extractor`, i.e., before the `def` line `@tsj_extractor` should be placed.
(A Python generator is a Python function that uses `yield` instead of `return` to produce an iterable of multiple results per call)
The input and output column types expected by the generator can be declared using the argument default values and `@returns` decorator, respectively.
They tell how the input parser and output formatter should behave.

Let's look at a realistic example to describe how exactly they should be used in the code.
Below is a near-complete code for the `udf/classify.py` declared as the implementation for the DDlog function `classify_articles`.

```python
#!/usr/bin/env python
from deepdive import *  # Required for @tsj_extractor and @returns

compsci_authors = [...]
bio_authors     = [...]
bio_words       = [...]

@tsj_extractor  # Declares the generator below as the main function to call
@returns(lambda # Declares the types of output columns as declared in DDlog
        article_id = "int",
        topic      = "text",
    :[])
def classify(   # The input types can be declared directly on each parameter as its default value
        article_id = "int",
        author     = "text",
        words      = "text[]",
    ):
    """
    Classify articles by assigning topics.
    """
    num_topics = 0

    if author in compsci_authors:
        num_topics += 1
        yield [article_id, "cs"]

    if author in bio_authors:
        num_topics += 1
        yield [article_id, "bio"]
    elif any (word for word in bio_words if word in words):
        num_topics += 1
        yield [article_id, "bio"]

    if num_topics == 0:
        yield [article_id, None]
```

This simple UDF assigns a topic to articles based on their authors' membership in known categories.
If the author is not recognized, we try to look for words that appear in a predefined set.
Finally, if nothing matches, we simply put it into another catch-all topic.
Note that the topics themselves here are completely user defined.

Notice that to use these Python decorators you'll need to have `from deepdive import *`.
Also notice that the types of input columns can be declared as default values for the generator parameters in the same way as `@returns`.

### @tsj_extractor decorators

The `@tsj_extractor` decorator should be placed as the first decorator for the main generator that will take one input row at a time and `yield` zero or more output rows as list of values.
This basically lets DeepDive know which function to call when running the Python program.
(For TSV, there's `@tsv_extractor` decorator, but TSJ is strongly recommended.)

#### Caveats
Generally, this generator should be placed at the bottom of the program unless there are some cleanup or tear-down tasks to do after processing all the input rows.
Any function or variable used by the decorated generator should be appear before it as the `@tsj_extractor` decorator will immediately start parsing input and calling the generator.
The generator should not `print` or `sys.stdout.write` anything as that will corrupt the standard output.
Instead, `print >>sys.stderr` or `sys.stderr.write` can be used for logging useful information.
More information can be found in the [debugging-udf](debugging-udf.md) section

### Parameter default values and @returns decorator

To parse the input TSJ lines correctly into Python values and format the values generated by the `@tsj_extractor` correctly in TSJ the column types need to be written down in the Python program.
They should be consistent with the function declaration in DDlog.
The types for input columns can be declared directly in the `@tsj_extractor` generator's signature as parameter default values as shown in the example above.
Arguments to the `@returns` decorator can be either a list of name and type pairs or a function with all parameters having their default values set as its type.
The use of `lambda` is preferred because the list of pairs require more symbols that clutter the declaration.
For example, compare above with `@returns([("article_id", "int"), ("topic", "text")])`.
The reason `dict(column="type", ... )` or `{ "column": "type", ... }` do not work is because Python forgets the order of the columns with those syntax, which is crucial for the TSJ parser and formatter.
The passed function is never called so the body can be left as any value, such as empty list (`[]`).
DeepDive also provides an `@over` decorator for input columns symmetric to `@returns`, but use of this is not recommended as it incurs redundant declarations.


## Running and debugging UDFs

Once a first cut of the UDF is written, it can be run using the `deepdive do` and `deepdive redo` commands.
For example, the `classify_articles` function in our running example to derive the `classification` relation can be run with the following command:

```bash
deepdive redo classification
```

This will invoke the Python program `udf/classify.py`, giving as input the TSJ rows holding three columns of the `article` table, and taking its output to add rows to the `classification` table in the database.

There are dedicated pages describing more details about [running these UDFs](ops-execution.md) and [debugging these UDFs](debugging-udf.md).


<!-- TODO Mention deepdive testfire or deepdive check here once it's ready -->
