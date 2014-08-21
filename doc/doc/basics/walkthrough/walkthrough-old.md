---
layout: default
---

# Example Application: A Mention-Level Extraction System

In this document, we will walk through how to **build an application to extract "spouse" relation from text** in DeepDive. By following this document you can learn about basic DeepDive functionality.

If you are not familiar with basic concepts in DeepDive (such as feature extraction and inference rules), please read our [homepage]({{site.baseurl}}/index.html) and this [overview](overview.html) first.

### Introduction

A typical use case for DeepDive is [Relation Extraction](general/relation_extraction.html). This tutorial will walk you through building a full DeepDive application that extracts `has_spouse` relationships from raw text. We use news articles as our input data and want to extract all pairs of people that participate in a `has_spouse` relation, for example *Barack Obama* and *Michelle Obama*. This example should can be easily translated into relation extraction in other domains, such as extracting interactions between drugs, or relationships among companies.

In the rest of this documentation, we will first install DeepDive on your machine, set up a database, and create a folder `app/spouse` in your DeepDive directory. Then we load prepared data from `examples/spouse_example/data` into our database, write [extractors](extractors.html) in our application to extract features, add [inference rule](inference_rules.html) into it. Finally we discuss how to interpret the results and improve the extraction quality.

The full application is also available in the folder `/examples/spouse_example`, which contains all possible implementations in [different types of extractors](extractors.html). In this document, we only introduce the default extractor type (json_extractor), which correspond to `/examples/spouse_example/default_extractor` in your DeepDive directory.

### Contents


1. [High-level picture of the application](#high_level_picture)
2. [Installing DeepDive](#installing)
3. [Setting up the database](#setup)
4. [Creating a new DeepDive application](#newapp)
5. [Loading initial data](#loading_data)
6. [Adding a people extractor](#people_extractor)
7. [Extracting candidate relations](#candidate_relations)
8. [Adding Features for candidate relations](#candidate_relation_features)
9. [Writing inference rules](#inference_rules)
10. [Evaluating the result](#evaluation)

Appendix:

11. [Using NLP extractor](#nlp_extractor)
12. [Using pipelines](#pipelines)



<a id="high_level_picture" href="#"> </a>

### High-level picture of the application

We will be building an application that extract spouse relation of people from raw text.

On a high level, the application we want to build will perform following steps on the data:

1. Extract mentions of people in the text
2. Extract all candidate pairs of people that possibly participate in a `has_spouse` relation
3. Label some of the `has_spouse` candidate pairs as correct or incorrect, based on a knowledge base. (i.e. [distant supervision](general/relation_extraction.html))
4. Extract features for `has_spouse` candidates to help prediction
5. Write inference rules to incorporate domain knowledge that improves our predictions

For simplicity, we will start from some preprocessed sentence data. If our input is raw text articles, we also need to run natural language processing in order to extract candidate pairs and features. If you want to learn how NLP extraction can be done in DeepDive, you can refer to the last section later: [using NLP extractor in DeepDive](#nlp_extractor). 


<a id="installing" href="#"> </a>

### Installing DeepDive

Start by [downloading and installing DeepDive on your machine](installation.html). In the rest of this tutorial we will assume that you have a `deepdive` directory as your working directory.


<a id="setup" href="#"> </a>

### Setting up the database

We will be using PostgreSQL as our primary database in this example. If you followed the DeepDive installation guide and passed all tests then your PostgreSQL server should be running already. Let's start by creating a new database called `deepdive_spouse` by typing in command line:

{% highlight bash %}
createdb deepdive_spouse
{% endhighlight %}

<div id="newapp" href="#"> </div>

### Creating a new DeepDive application

Start by creating a new folder `app/spouse` in the DeepDive directory for your application.

{% highlight bash %}
cd deepdive
mkdir app
mkdir app/spouse
cd app/spouse
{% endhighlight %}

DeepDive's main entry point is a file called `application.conf` which contains database connection information as well as your feature extraction and inference rule pipelines. It is often useful to have a small `run.sh` script that loads environment variables and executes the DeepDive pipeline. We provide simple templates for both of these to copy and modify. Copy these templates to our directory by the following commands: 

<!-- TODO what is env.sh doing? -->

{% highlight bash %}
cp ../../examples/template/application.conf application.conf
cp ../../examples/template/run.sh run.sh
cp ../../examples/template/env.sh env.sh
{% endhighlight %}

The `env.sh` file configures environment variables that will be used in this application. Start modifying the `env.sh` file with your database name:
  
{% highlight bash %}
# File: env.sh
export DBNAME=deepdive_spouse
{% endhighlight %}

Note that in the `run.sh` file it defines environment variables `APP_HOME` which is our current directory `app/spouse`, and `DEEPDIVE_HOME` which is the home directory of DeepDive. We will use these variables later.

You can now try executing the `run.sh` file. Because you have not defined any extractors or inference rules you will not see meaningful results, but DeepDive should run successfully from end to end and you should be able to see a summary report such as:

    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  Summary Report
    15:57:55 [profiler] INFO  --------------------------------------------------


<a id="loading_data" href="#"> </a>

### Loading initial data

In this example we will be using raw text from a couple of New York Times articles. Note that there is nothing special about our data set, and you are free to use whatever raw text data you want. Let's copy the data into our directory, and load it into the database. 

Type in following commands to create a table:

{% highlight bash %}
psql -d deepdive_spouse -c "
  CREATE TABLE articles(
    article_id bigint,
    text       text
  );
"
{% endhighlight %}

Copy prepared data from DeepDive `example/spouse_example/data` folder into `app/spouse/data`:

{% highlight bash %}
cp -r ../../examples/spouse_example/data ./data
{% endhighlight %}

The `data` folder contains several starter dumps:

- `articles_dump.csv` contains initial data: articles we extract relation from. We will just start from the parsed sentences dataset:
- `sentences_dump.csv` contains all parsed sentences from these articles. If you want to know how to get this dataset from articles, refer to [NLP extractor](#nlp_extractor) section.
- `spouses.csv` and `non-spouses.tsv` Freebase relations we will use for distant supervision. We will come to them later.

First we create a `sentences` table in our database by typing: 

{% highlight bash %}  
psql -d deepdive_spouse -c "
  CREATE TABLE sentences(
    document_id  bigint,  -- which document it comes from
    sentence     text,    -- sentence content
    words        text[],  -- array of words in this sentence
    lemma        text[],  -- array of lemmatized words
    pos_tags     text[],  -- array of part-of-speech tags
    dependencies text[],  -- array of dependency paths
    ner_tags     text[],  -- array of named entity tags (PERSON, LOCATION, etc)
    sentence_id  bigint   -- unique identifier for sentences
    );
"
{% endhighlight %}

Then we load prepared sentences into our database:

{% highlight bash %}
psql -d deepdive_spouse -c "
  COPY sentences(sentence_id, document_id, sentence, words, lemma, pos_tags, dependencies, ner_tags)
  FROM STDIN CSV;
" < ./data/sentences_dump.csv
{% endhighlight %}

Here we go! We have all sentences prepared in our database. (feel free to check how they look like)  Now we are ready to go!


<a id="people_extractor" href="#"> </a>

### Adding a people extractor

Our next task is to write several [extractors](extractors.html) in
DeepDive to transform initial data into the format we need. On a high
level, each extractor performs a user-defined function (UDF) on an input
query against database, **in a row-wise manner**. One may think of an
extractor as a function which *maps one input tuple (one row in input
SQL query)* to one or more output tuples, similar to a `map` or
`flatMap` function in functional programming languages (or `map` in
MapReduce).

Our first extractor will extract people mentions from the sentences, and put them into a new table. 
Note that we have named entity tags in column `ner_tags` of our `sentences` table. We will use this column to identify people mentions: we assume that *a word phrase is a people mention if all its words are tagged as `PERSON` in its `ner_tags` field.*

<!-- Ideally you would want to add your own domain-specific features to extract mentions. For example, people names are usually capitalized, tagged with a noun phrase part of speech tag, and have certain dependency paths to other words in the sentence. However, because the Stanford NLP Parser is relatively good at identifying people and tags them with a `PERSON` named-entity tag we trust its output and don't make the predictions ourselves. We simply assume that all people identified by the NLP Parser are correct. Note that this assumption is not ideal and usually does not work for other types of entities, but it is good enough to build a first version of our application.
 -->

Again, we first create a new table in the database by typing:
  
{% highlight bash %}
psql -d deepdive_spouse -c "
  CREATE TABLE people_mentions(
    sentence_id    bigint, -- refers to sentences table
    start_position int,    -- word offset in the sentence
    length         int,    -- how many words in this mention
    text           text,   -- name of the person
    mention_id     bigint  -- unique identifier for people_mentions
  );
"
{% endhighlight %}

Let's tell DeepDive to use our first extractor, by adding the following lines into `deepdive.extraction.extractors` block in `application.conf`, which should be present in the template:

    deepdive {
      ...
      # Put your extractors here
      extraction.extractors {
        
        ext_people {
          input: """
              SELECT  sentence_id, words, ner_tags
              FROM    sentences
              """
          output_relation : "people_mentions"
          udf             : ${APP_HOME}"/udf/ext_people.py"
          before          : ${APP_HOME}"/udf/clear_table.sh people_mentions"
          after           : ${APP_HOME}"/udf/fill_sequence.sh people_mentions mention_id"
        }
        # ... (more extractors to add here)
      } 
      ...   
    }

Let's go through each line:

1. The input to the `ext_people` extractor are all sentences (identifiers, words and named entity tags), selected using a SQL statement.
2. The output of the extractor will be written to the `people_mentions` table.
3. The extractor script is `udf/ext_people.py`. DeepDive will execute this command and stream input to the *stdin* of the process, and read output from *stdout* of the process.
4. We execute a script *before* the extractor runs, and another script *after* the extractor runs.

There are other ways you can use an extractor, refer to the [extractor guide](extractors.html) for a more comprehensive list. 

We create a folder named `udf` to put our scripts:

{% highlight bash %}
mkdir udf
{% endhighlight %}

Then create a `udf/ext_people.py` script as the UDF for this extractor, which scan through input sentences and output phrases. The script can be written as follows:

{% highlight python %}
#! /usr/bin/env python
# File: udf/ext_people.py

import json, sys

# For each sentence
for row in sys.stdin:
  sentence_obj = json.loads(row)
  # Find phrases that are continuous words tagged with PERSON.
  phrases = []   # Store (start_position, length, text)
  words = sentence_obj["words"]         # a list of all words
  ner_tags = sentence_obj["ner_tags"]   # ner_tags for each word
  start_index = 0
  
  while start_index < len(words):
    # Checking if there is a PERSON phrase starting from start_index
    index = start_index
    while index < len(words) and ner_tags[index] == "PERSON": 
      index += 1
    if index != start_index:   # a person from "start_index" to "index"
      text = ' '.join(words[start_index:index])
      length = index - start_index
      phrases.append((start_index, length, text))
    start_index = index + 1
  
  # Output a tuple for each PERSON phrase
  for start_position, length, text in phrases:
    print json.dumps({
      "sentence_id": sentence_obj["sentence_id"],
      "start_position": start_position,
      "length": length,
      "text": text,
      "mention_id": None
    })

{% endhighlight %}

This `udf/ext_people.py` Python script takes sentences records as an input, and outputs a people record for each (potentially multi-word) person phrase found in the sentence, which are one or multiple continuous words tagged with `PERSON`. 

Note that if you wanted to add debug output, you can print to *stderr* instead of stdout, and the messages would appear in the log file.

At this point you may be wondering about the `before` and `after` script. Why do we need them? Each time before the extractor runs we want to clear out the `sentences` table and remove old data; after the extractor runs we want to fill the column `mention_id` with unique values for future tables to refer. These actions are pretty common in this application, so we make two general scripts, which are also used for future extractors:

Create `udf/clear_table.sh` as follows. This script truncates (clears) any input table:

{% highlight bash %}
# File: udf/clear_table.sh
# Usage: fill_sequence.sh  TABLE_NAME
psql -d deepdive_spouse -c "
  TRUNCATE $1 CASCADE;
"
{% endhighlight %}

Create `udf/fill_sequence.sh` as follows. This script fills a row in an input table with incremental, unique integers starting from 1:

{% highlight bash %}
# File: udf/fill_sequence.sh
# Usage: fill_sequence.sh  TABLE_NAME  COLUMN_NAME
# Postgres will use a random continuous sequence to fill your column starting from 1. The order is not guaranteed.
psql -d deepdive_spouse -c "
        DROP SEQUENCE IF EXISTS tmp_sequence_$1;
        CREATE SEQUENCE tmp_sequence_$1;
        UPDATE $1 SET $2 = nextval('tmp_sequence_$1');
        DROP SEQUENCE tmp_sequence_$1;
"
{% endhighlight %}

Don't forget to change permission to the new scripts you created by typing following commands:

{% highlight bash %}
chmod +x udf/clear_table.sh
chmod +x udf/fill_sequence.sh
{% endhighlight %}

If you want, you can now run your extractor by executing `./run.sh`, and you will be able to see extracted results in `people_mentions`.


<a id="candidate_relations" href="#"> </a>

### Extracting candidate relations between mention pairs

Now comes the interesting part! We have laid all the groundwork to extract the `has_spouse` relation we care about. Let's create a table for it by typing:

{% highlight bash %}
psql -d deepdive_spouse -c "
  CREATE TABLE has_spouse(
    person1_id  bigint,  -- first person's mention_id in people_mentions
    person2_id  bigint,  -- second person's mention_id
    sentence_id bigint,  -- which senence it appears
    description text,    -- a description of this relation pair
    is_true     boolean, -- whether this relation is correct
    relation_id bigint,  -- unique identifier for has_spouse
    id          bigint   -- reserved for DeepDive
  );
"
{% endhighlight %}

Note the special `is_true` column in the above table. We need this column because we want DeepDive to predict how likely it is that a given entry in the table is correct. In other words, DeepDive will create a [random variable](general/inference.html) for each instance of it. More concretely, each row in the `has_spouse` table will be assigned random variable for its `is_true` column. 

Also note that we must reserve another special column, `id bigint`, in any table containing variables like this one. For system to use, this column should be **left blank, and not be used anywhere**. We will further see syntax requirements in *inference rules* related to this `id` column.

Let's tell DeepDive to use the `is_true` column for probabilistic inference in the `application.conf`:

    schema.variables {
      has_spouse.is_true: Boolean
    }

Let's create an extractor that extracts all candidate relations and puts them into the above table. We call them *candidate relations* because we are not sure whether or not they are actually correct, that's for DeepDive to predict. We will be adding *features* to make predictions in the next step, for now we are just outputting all of the candidates.

    extraction.extractors {

      # ... (other extractors)

      ext_has_spouse_candidates {
        input: """
          SELECT  sentences.sentence_id,
                  p1.mention_id AS "p1.mention_id",
                  p1.text       AS "p1.text",
                  p2.mention_id AS "p2.mention_id",
                  p2.text       AS "p2.text"
           FROM   people_mentions p1,
                  people_mentions p2,
                  sentences
          WHERE   p1.sentence_id = p2.sentence_id
            AND   p1.sentence_id = sentences.sentence_id
            AND   p1.mention_id != p2.mention_id;
            """
        output_relation : "has_spouse"
        udf             : ${APP_HOME}"/udf/ext_has_spouse.py"
        before          : ${APP_HOME}"/udf/clear_table.sh has_spouse"
        after           : ${APP_HOME}"/udf/fill_sequence.sh has_spouse relation_id"
        dependencies    : ["ext_people"]
      }

    }

Note that this extractor must be executed after our last extractor `ext_people`, which is specified in "dependencies" field.

In this extractor, we select all pairs of people mentions that occur in the same sentence. When generating relation candidates, we also generate training data using [distant supervision](general/relation_extraction.html). There are some pairs of people that we know for sure are married, and we can use them as training data for DeepDive. Similarly, if we know that two people are not married, we can use them as negative training examples. In our case we will be using data from [Freebase](http://www.freebase.com/) for distant supervision. 

To generate positive examples, we have exported all pairs of people with a `has_spouse` relationship from the [Freebase data dump](https://developers.google.com/freebase/data) and included the CSV file in `data/spouses.csv`.

To generate negative examples, we include a TSV file in `data/non-spouses.tsv` containing these relations sampled from Freebase. Specifically, they contain:

1. Pairs of the same person, e.g., "Barack Obama" cannot be married to "Barack Obama". 
2. Pairs of other relations, e.g. if A is B's parent / children / sibling, A is not likely to be married to B. 

Let's create a script `udf/ext_has_spouse.py` as below, to generate and label the relation candidates:

{% highlight python %}
#! /usr/bin/env python
# File: udf/ext_has_spouse.py

import json, csv, os, sys
from collections import defaultdict

BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
spouses = {}
with open (BASE_DIR + "/../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    name1 = line[0].strip().lower()
    name2 = line[1].strip().lower()
    spouses[name1] = name2

# Load relations of people that are not spouse
non_spouses = set()
lines = open(BASE_DIR + '/../data/non-spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair

# For each input tuple
for row in sys.stdin:
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_id = obj["p1.mention_id"]
  p1_text = obj["p1.text"].strip()
  p1_lower = p1_text.lower()
  p2_id = obj["p2.mention_id"]
  p2_text = obj["p2.text"].strip()
  p2_lower = p2_text.lower()
  sentence_id = obj["sentence_id"]

  # See if the combination of people is in our supervision dictionary
  # If so, set is_correct to true or false
  is_true = None
  if p1_lower in spouses and spouses[p1_lower] == p2_lower:
    is_true = True
  if p2_lower in spouses and spouses[p2_lower] == p1_lower:
    is_true = True
  # appear in other relations
  elif (p1_lower, p2_lower) in non_spouses: 
    is_true = False
  elif (p2_lower, p1_lower) in non_spouses:
    is_true = False
  # same person
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = False

  print json.dumps({
    "person1_id": p1_id,
    "person2_id": p2_id,
    "sentence_id": sentence_id,
    "description": "%s-%s" %(p1_text, p2_text),
    "is_true": is_true,
    "relation_id": None,
    "id": None
  })
{% endhighlight %}


<a id="candidate_relation_features" href="#"> </a>

### Adding Features for candidate relations

For DeepDive to make predictions, we need to add *features* to our candidate relations. Features are properties that help decide whether or not the given relation is correct. For example, one feature may be the sequence of words between the two mentions. We could have saved the features in the `has_spouse` table that we created above, but it is often cleaner to have a separate table for them.

Create a table by typing:

{% highlight bash %}
psql -d deepdive_spouse -c "
  CREATE TABLE has_spouse_features(
    relation_id bigint,
    feature     text
  );
"
{% endhighlight %}

And create a new extractor for features, which will execute after our last `ext_has_spouse_candidates` extractor:

    extraction.extractors {

      # ... (other extractors)

      ext_has_spouse_features {
        input: """
          SELECT  sentences.words,
                  has_spouse.relation_id,
                  p1.start_position  AS  "p1.start_position",
                  p1.length          AS  "p1.length",
                  p2.start_position  AS  "p2.start_position",
                  p2.length          AS  "p2.length"
            FROM  has_spouse,
                  people_mentions p1,
                  people_mentions p2,
                  sentences
           WHERE  has_spouse.person1_id = p1.mention_id
             AND  has_spouse.person2_id = p2.mention_id
             AND  has_spouse.sentence_id = sentences.sentence_id;
             """
        output_relation : "has_spouse_features"
        udf             : ${APP_HOME}"/udf/ext_has_spouse_features.py"
        before          : ${APP_HOME}"/udf/before_has_spouse_features.sh"
        dependencies    : ["ext_has_spouse_candidates"]
      }

    }

Create script `udf/ext_has_spouse_features.py` as follows:

{% highlight python %}
#! /usr/bin/env python
# File: udf/ext_has_spouse_features.py

import sys, json

# For each input tuple
for row in sys.stdin:
  obj = json.loads(row)

  p1_start = obj["p1.start_position"]
  p1_length = obj["p1.length"]
  p1_end = p1_start + p1_length
  p2_start = obj["p2.start_position"]
  p2_length = obj["p2.length"]
  p2_end = p2_start + p2_length

  p1_text = obj["words"][p1_start:p1_length]
  p2_text = obj["words"][p2_start:p2_length]

  # Features for this pair come in here
  features = set()
  
  # Feature 1: Words between the two phrases
  left_idx = min(p1_end, p2_end)
  right_idx = max(p1_start, p2_start)
  words_between = obj["words"][left_idx:right_idx]
  if words_between: 
    features.add("words_between=" + "-".join(words_between))

  # Feature 2: Number of words between the two phrases
  features.add("num_words_between=%s" % len(words_between))

  # Feature 3: Does the last word (last name) match assuming the words are not equal?
  last_word_left = obj["words"][p1_end - 1]
  last_word_right = obj["words"][p2_end - 1]
  if (last_word_left == last_word_right) and (p1_text != p2_text):
    features.add("last_word_matches")

  # TODO: Add more features, look at dependency paths, etc

  for feature in features:  
    print json.dumps({
      "relation_id": obj["relation_id"],
      "feature": feature
    })
{% endhighlight %}

Create script `udf/before_has_spouse_features.sh` as follows:

{% highlight bash %}
#! /usr/bin/env bash
psql -c "TRUNCATE has_spouse_features CASCADE;" deepdive_spouse
{% endhighlight %}



<a id="inference_rules" href="#"> </a>

### Writing inference rules

Now we need to tell DeepDive how to perform [probabilistic inference](general/inference.html) on the data we have generated.  We want to predict the `is_true` column of the `has_spouse` table based on the features we have extracted. This is the simplest rule you can write, because it does not involve domain knowledge or relationships among variables. Add the following to your `application.conf`:

    inference.factors {

      f_has_spouse_features {
        input_query: """
          SELECT has_spouse.id      AS "has_spouse.id",
                 has_spouse.is_true AS "has_spouse.is_true",
                 feature
          FROM has_spouse,
               has_spouse_features
          WHERE has_spouse_features.relation_id = has_spouse.relation_id
          """
        function : "IsTrue(has_spouse.is_true)"
        weight   : "?(feature)"
      }

    }

This rule generates a model similar to a logistic regression classifier. We use a set of features to make a prediction about the variable we care about. For each row in the *input query* we are adding a [factor](general/inference.html) that connects to the `has_spouse.is_true` variable with a different weight for each feature name. 

Note that the syntax requires users to explicitly select:

1. `id` column for each variable
2. The variable column, which is `is_true` in this case
3. The column weight is dependent on, which is `feature` in this case

And when selecting them, users must explicitly alias `id` to `[relation_name].id` and `[variable]` to `[relation_name].[variable]` for system to use. See more in [inference rule guide](inference_rules.html).

Before getting results, let's try to incorporate a bit of domain knowledge into our model. For example, we know that has_spouse is symmetric. That means, if Barack Obama is married to Michelle Obama, then Michelle Obama is married to Barack Obama, and vice versa. (`Marry(A,B) <-> Marry(B,A)`) We can encode this knowledge in a second inference rule:

    inference.factors {

      # ...(other inference rules)

      f_has_spouse_symmetry {
        input_query: """
          SELECT r1.is_true AS "has_spouse.r1.is_true",
                 r2.is_true AS "has_spouse.r2.is_true",
                 r1.id      AS "has_spouse.r1.id",
                 r2.id      AS "has_spouse.r2.id"
          FROM has_spouse r1,
               has_spouse r2
          WHERE r1.person1_id = r2.person2_id
            AND r1.person2_id = r2.person1_id
          """
        function: "Equal(has_spouse.r1.is_true, has_spouse.r2.is_true)"
        weight: "?"
      }

    }


There are many [other kinds of factor functions](inference_rule_functions.html) you could use to encode domain knowledge. 


We are almost ready to run! In order to evaluate our results, we also want to define a *holdout fraction* for our predictions. The holdout fraction defines how much of our training data we want to treat as testing data used to compare our predictions against. By default the holdout fraction is `0`, which means that we cannot evaluate the precision of our results. Add the following line to holdout 1/4 of the training data:

    calibration.holdout_fraction: 0.25

<!-- Finally, there is one last technical details we need to pay attention to. Our first rule is very sparse, in other words, the same features usually only applies to a few candidates that we extract. Our second rule on the other hand applies to every candidate, which means that DeepDive will learn a very high weight for it. This discrepancy can lead to longer convergence times during the inference step if we don't select a correct "learning rate". For such a case, let's change the learning rate from the default of "0.1" to the "0.001" by adding the following sampler options to the configuration file:

    sampler.sampler_args: "-l 125 -s 1 -i 200 --alpha 0.001"
 -->


<a id="evaluation" href="#"> </a>

### Evaluating the result

Let's try running the full pipeline using `./run.sh`. (If you want to run part of the pipeline, refer to [this section](#pipelines))

After running, you should see a summary report similar to:

    04:07:48 [profiler] INFO  --------------------------------------------------
    04:07:48 [profiler] INFO  Summary Report
    04:07:48 [profiler] INFO  --------------------------------------------------
    04:07:48 [profiler] INFO  ext_people SUCCESS [43958 ms]
    04:07:48 [profiler] INFO  ext_has_spouse_candidates SUCCESS [22376 ms]
    04:07:48 [profiler] INFO  ext_has_spouse_features SUCCESS [84158 ms]
    04:07:48 [profiler] INFO  inference_grounding SUCCESS [6175 ms]
    04:07:48 [profiler] INFO  inference SUCCESS [10293 ms]
    04:07:48 [profiler] INFO  calibration plot written to /YOUR/PATH/TO/deepdive/out/TIME/calibration/has_spouse.is_true.png [0 ms]
    04:07:48 [profiler] INFO  calibration SUCCESS [538 ms]
    04:07:48 [profiler] INFO  --------------------------------------------------

Great, let's take a look at some of the predictions that DeepDive has made. DeepDive creates a view `has_spouse_is_true_inference` for each variable you have defined in the database. Type in following query in command line to sample some predictions with high confidence:

{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT sentence_id, description, expectation
  FROM has_spouse_is_true_inference
  WHERE expectation > 0.9
  LIMIT 10;
"
{% endhighlight %}

The result should be something like (might not be the same):

     sentence_id |             description             | expectation
    -------------+-------------------------------------+-------------
           77558 | Calvin Coolidge-Grace Coolidge      |           1
           84350 | Cherie Blair-Tony Blair             |           1
           64209 | Cecilia-Sarkozy                     |           1
           84337 | Tony Blair-Cherie Blair             |           1
           70974 | Dustin Hoffman-Natalie Portman      |       0.998
           67407 | Dustin Hoffman-Dennis Quaid         |       0.998
           52334 | Mary Matalin-James Carville         |       0.998
           78704 | Bill Clinton-Hillary Rodham Clinton |       0.998
           48282 | Clinton-Obama                       |       0.998
           47994 | Lauren Bacall-Jason Robards         |       0.998
    (10 rows)


We can see that some of these tuples are actually correct instances of married people. (Among above results, row 1, 2, 7, 8, 10 are correct)

How can we improve these predictions? DeepDive generates [calibration plots](general/calibration.html) for all variables defined in the schema to help with debugging. Let's take a look at the generated calibration plot, written to the file outputted in the summary report above. It should look something like this:

![Calibration]({{site.baseurl}}/assets/walkthrough_has_spouse_is_true.png)

The calibration plot contains useful information that help you to improve the quality of your predictions. For actionable advice about interpreting calibration plots, refer to the [calibration guide](general/calibration.html). 

Often, it is also useful to look at the *weights* that were learned for features or rules. You can do this by looking at the `mapped_inference_results_weights` table in the database. Type in following command to select features with highest weight (positive features):

{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_variables_mapped_weights
  ORDER BY weight DESC
  LIMIT 5;
"
{% endhighlight %}

                                      description                                   |      weight
    --------------------------------------------------------------------------------+------------------
     f_has_spouse_features-words_between=and-his-wife-,                             | 4.57022662570932
     f_has_spouse_features-words_between=and-former-President                       | 3.71843537330123
     f_has_spouse_features-words_between=,-the-wife-of-Sen.                         | 3.33624750158969
     f_has_spouse_features-words_between=,-the-widower-of                           | 3.19681498554069
     f_has_spouse_features-words_between=faced-as-president-,-few-stand-out-to-Sen. | 2.94090009406733
    (5 rows)

Type in following command to select top negative features:

{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_variables_mapped_weights
  ORDER BY weight ASC
  LIMIT 5;
"
{% endhighlight %}

                                             description                                         |      weight
    ---------------------------------------------------------------------------------------------+-------------------
     f_has_spouse_features-words_between='s-father-,                                             | -2.08542859712276
     f_has_spouse_features-words_between=;-director-of-photography-,                             | -2.05141101637116
     f_has_spouse_features-words_between='s-second-wife-,-Cecilia--RRB--,-were-the-witnesses-for | -2.01128332145738
     f_has_spouse_features-words_between=and-Obama-shook-hands-,-and                             | -1.97077846059915
     f_has_spouse_features-words_between=;-written-by                                            | -1.95299763087218
    (5 rows)

Note that each execution may learn different weights, and these lists can look different. Generally, we might see that most weights make sense while some don't, which is OK for our first application. 

**Congratulations!!** Now we have already finished our first working relation extractor!

You can further improve the prediction by different ways. There are many possible strategies including:

- Making use of co-reference information
- Performing entity linking instead of extraction relations among mentions in the text
- Adding more inference rules that encode your domain knowledge
- Adding more (or better) positive or negative training examples
- Adding more (or better) features

For the second point: our goal in this tutorial is get an initial
application up and running. There are a couple of problems with the
approach above which are worth drawing attention to: If two separate
sentences mention the fact that Barack Obama and Michelle Obama are in a
`has_spouse` relationship, then our approach does not know that they
refer to the same fact. In other words, we ignore the fact that "Barack
Obama" and "Michelle Obama" in both of these sentence refer to the same
entity in the real world. We also don't recognize *coreference* of two
mentions. That is, we don't know that "Barack Obama" and "Obama"
probably refer to the same person. We will address these issues in the
[advanced part of the tutorial](walkthrough2.html).


<!-- Here we can see that the word phrase "and-former-President" in between the two person names has a rather high weight. This seems strange, since this phrase is not an indicator of a marriage relationship. One way to improve our predictions would be to add more negative evidence that would lower the weight of that feature.
 -->


<!-- TODO!!!! -->

<a id="nlp_extractor" href="#"> </a>


----

## Appendix

### Adding an NLP extractor

If you want, you can try extracting the `sentences` table yourself. This should be useful if you want to extract your own dataset.

To start from an NLP extractor, we first load all the articles into our database:

{% highlight bash %}
psql -d deepdive_spouse -c "
  COPY articles FROM STDIN CSV;
" < data/articles_dump.csv
{% endhighlight %}


The first step towards performing Entity and Relation Extraction is to extract natural language features from the raw text. This is usually done using an NLP library such as [the Stanford Parser](http://nlp.stanford.edu/software/lex-parser.shtml) or [NLTK](http://nltk.org/). Because natural language processing is such a common first step we provide a pre-built extractor which uses the [Stanford CoreNLP Kit](http://nlp.stanford.edu/software/corenlp.shtml) to split documents into sentences and tag them. Let's copy it into our project. 

The NLP extractor we provide lies in `examples/nlp_extractor` folder. Refer to its `README.md` for details. Now we go into it and compile it:
  
    cd ../../examples/nlp_extractor
    sbt stage
    cd ../../app/spouse

The `sbt stage` command compiles the extractor (written in Scala) and generates a handy start script. The extractor itself takes JSON tuples of raw document text as input, and outputs JSON tuples of sentences with information such as part-of-speech tags and dependency parses. Let's now create a new table for the output of the extractor in our database. Because the output format of the NLP extractor is fixed by us, you must create a compatible table, like `sentences` defined [above](#loading_data).

Next, add the extractor: 

    extraction.extractors {

      ext_sentences: {
        input: """
          SELECT  article_id, text
          FROM    articles
          ORDER BY article_id ASC
          """
        output_relation : "sentences"
        udf             : "examples/nlp_extractor/run.sh -k article_id -v text -l 20 -t 4"
        before          : ${APP_HOME}"/udf/before_sentences.sh"
        after           : ${APP_HOME}"/util/fill_sequence.sh sentences sentence_id"
      }

      # ... More extractors to add dere
    }

(Make sure this extractor is executed before `ext_people` by adding dependencies to the latter.)


<a id="pipelines" href="#"> </a>

### Using pipelines

By default, DeepDive runs all extractors that are defined in the configuration file. Sometimes you only want to run some of your extractors to test them, or to save time when the output of an early extractor hasn't changed. The NLP extractor is a good example of this. It takes a long time to run, and its output will be the same every time, so we don't want to run it more than once. DeepDive allows you to define different [pipelines](pipelines.html) for this purpose, by adding the following to your `application.conf`:

    pipeline.pipelines.withnlp: [
      "ext_sentences",    # NLP extractor, takes very long
      "ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features",
      "f_has_spouse_features", "f_has_spouse_symmetry"
    ]

    pipeline.pipelines.nonlp: [
      "ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features",
      "f_has_spouse_features", "f_has_spouse_symmetry"
    ]

The code above created two pipelines, one with NLP extraction and the other without NLP. We further add a line:


    pipeline.run: "nonlp"

This will tell DeepDive to execute the "nonlp" pipeline, which only contains the "ext_people" extractor.

