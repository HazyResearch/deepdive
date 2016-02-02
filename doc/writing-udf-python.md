---
layout: default
title: Writing user-defined functions in Python
---

# Writing user-defined functions in Python

In the previous section [Defining Data Processing in DDlog](writing-dataflow-ddlog.md) we saw that we can write UDFs for various purposes including extraction and supervision.  We saw how to define the function in ddlog and to call it in order to write it's output to a defined relation.  In this section we will look into the python files to see an example structure and see some utilities that DeepDive provides to make writing UDFs easier.  


Note that DeepDive supports running any executable but here we demonstrate an example in Python because DeepDive's ddlib provides utilities to easily parse the input rows and format the output rows.


## General Guidelines

Let's continue the example from [Defining Data Processing in DDlog](writing-dataflow-ddlog.md) and see what the UDF looks like in Python.  For convenience, we'll display the two relations, the function in ddlog and the function call here.

```
article(
  id int,
  length int,
  author text,
  words text[]).
  
classified_articles(
  article_id int,
  class text).
  
function classify_articles over (id int, author text, length int, words text[])
  return rows like article
  implementation "udf/classify.py" handles tsv lines.
  
classified_articles += classify_articles(id, author, length, words) :-
  article(id, length, author words)
```

Given the above app.ddlog we can run `deepdive do classify_articles`.  This will envoke a python script called 'udf/classify.py' giving as input the rows in tsv format from 'article' and expecting an output that will fit into classified articles (namely, a tsv row with an int and a string for the article_id and class).  A separate page outlines how to [debug UDFs](debugging-udf.md).  

## ddlib - @tsv_extractor, @returns, default parameters

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
        class            = "text", 
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


### @returns


### Default parameters
