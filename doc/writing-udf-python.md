---
layout: default
title: Writing user-defined functions in Python
---

# Writing user-defined functions in Python

DeepDive supports *user-defined functions* (UDFs) for data processing, in addition to the [normal derivation rules in DDlog](writing-dataflow-ddlog.md).
These functions are supposed to take as input tab-separated values ([PostgreSQL's TSV or text format](http://www.postgresql.org/docs/9.1/static/sql-copy.html#AEN64351)) per line and output zero or more lines also containing values separated by tab.
DeepDive provides a handy Python library for quickly writing the UDFs in a clean way as well as parsing and formating such input/output.
This page describes DeepDive's recommended way of writing UDFs in Python as well as how such UDFs are used in the DDlog program.
However, DeepDive is language agnostic when it comes to UDFs, i.e., a UDF can be implemented in any programming language as long as it can be executed from the command line, reads TSV lines correctly from standard input, and writes TSV to standard output.


## Using UDFs in DDlog

To use user-defined functions in DDlog, they must be declared first then called using special syntax.

First, let's declare the schema of the two relations for our running example.

```
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

Suppose we want to write a simple UDF to classify each `article` into different topics, adding tuples to the relation `classification`.


### Function Declarations
A function declaration states what input it takes and what output it returns as well as the implementation details.
In our example, suppose we will use only the `author` and `words` of each `article` to output the topic identified by its `id`, and the implementation will be kept in an executable file called `udf/classify.py`.
The exact declaration for such function is shown below.

```
function classify_articles over (id int, author text, words text[])
    returns (article_id int, topic text)
    implementation "udf/classify.py" handles tsv lines.
```

Notice that the column definitions of relation `classification` are repeated in the `returns` clause.
This can be omitted by using the `rows like` syntax as shown below.

```
function classify_articles over (id int, author text, words text[])
    return rows like classification
    implementation "udf/classify.py" handles tsv lines.
```

Also note that the function input is similar to the `articles` relation, but some columns are missing.
This is because the function will not use the rest of the columns as mentioned before, and it is a good idea to drop unnecessary values for efficiency.
Next section shows how such input tuples can be derived and fed into a function.


### Function Call Rules
The function declared above can be called to derive tuples for another relation of the output type.
The input tuples for the function call are derived using a syntax similar to a [normal derivation rule](writing-dataflow-ddlog.md).
For example, the rule shown below calls the `classify_articles` function to fill the `classification` relation using a subset of columns from the `articles` relation.

```
classification += classify_articles(id, author, words) :-
    article(id, _, _, author, words).
```

Function call rules can be thought as a special case of normal derivation rules with different head syntax, where instead of the head relation name, there is a function name after the name of the relation being derived separated by `+=`.


<todo>finish revision below</todo>

## Writing UDFs in Python

DeepDive (specifically ddlib) provides some function decorators to simplify parsing and formatting input and output respectively.  Let's look an example python script and then describe the components.  Note that if you plan to use these features you'll need to use @tsv_extractor, @returns and set default parameters for all the parameters of that function.  You must include all three components discribed below or else this will not work.

This trival UDF checks to see if the authors of the article are in a known set and assigns a class based on that.  If the author is not recognized, we try to look for words that appear in a predefined set.  Finally, if nothing matches, we simply set another catch all class.  Note that the classes themselves here are completely user defined.

```
#!/usr/bin/env python
from deepdive import *

compsci_authors = [...]
bio_authors = [...]
bio_words = [...]

@tsv_extractor
@returns(lambda
        article_id       = "int",
        classification   = "text",
    :[])
def extract(article_id="int", author="text", length="int", words="text[]"):
    """
    Classify articles.
    """
    if author in compsci_authors:
      yeild [article_id, 'cs']
    if author in bio_authors:
      yeild [article_id, 'bio']

    for word in words:
      if word in bio_words:
        yeild [article_id, 'bio']

    yeild [article_id, 'unknown']
```

Note that for these decorators you'll need to `import deepdive`

### @tsv_extractor

The `@tsv_extractor` decoration can be used on the main function that will take the input and return the output as defined in ddlog.  This lets DeepDive know which function to call when running the script.

### @returns

Similar to the ddlog definition, ddlib needs to know what format your output will be from the `@tsv_extractor` function to correctly format it into tsv.  It is passed a lambda function which has as its parameters the fields that will be output.  The default value of the parameter must be the type of that parameter.  The body of the lambda is ignored and can be left as an empty list (`[]`) in all cases.

### Default parameters

Similar to the `@returns` decorator above, ddlib also needs to know what is the input the function is expecting.  However, in this case, we leverage the function signature to define the input.  The parameters are the correctly ordered fields of the relation that is being input to the function.  The default value for each parameter is the data type of that field.


<todo>

Given the above app.ddlog we can run `deepdive do classify_articles`.
This will envoke a python script called 'udf/classify.py' giving as input the rows in tsv format from 'article' and expecting an output that will fit into classified articles (namely, a tsv row with an int and a string for the article_id and class).
A separate page outlines how to [debug UDFs](debugging-udf.md).

</todo>
