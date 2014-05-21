---
layout: default
---

# Example Application: A Mention-Level Extraction System

In this document, we will walk through how to **build an application to extract mention-level `has_spouse` relation from text** in DeepDive. By following this document you can learn about basic DeepDive functionality.

If you are not familiar with basic concepts in DeepDive (such as feature extraction and inference rules), please start from the [walkthrough](walkthrough.html).

<!-- read our [homepage]({{site.baseurl}}/index.html) and this [overview](overview.html) first. -->

<a id="high_level_picture" href="#"> </a>

## High-level picture of the application

This tutorial will walk you through building a full DeepDive application that extracts **mention-level** `has_spouse` relationships from raw text. We use news articles as our input data and want to extract all pairs of people that participate in a `has_spouse` relation, for example *Barack Obama* and *Michelle Obama*. This example should can be easily translated into relation extraction in other domains, such as extracting interactions between drugs, or relationships among companies.

Note that in this section we are going to build an application that extract *plausible* mention-level relations, but *with rather low quality*.

<a id="dataflow" href="#"> </a>

On a high level, the application will perform following steps on the data:

1. Data preprocessing: prepare parsed sentences.
2. Feature extraction: 
  - Extract mentions of people in the text
  - Extract all candidate pairs of people that possibly participate in a `has_spouse` relation
  - Extract features for `has_spouse` candidates
  - Prepare training data by [distant supervision](general/relation_extraction.html) using a knowledge base
3. Generate factor graphs by inference rules
4. statistical inference and learning
5. Generate results

For simplicity, we will start from preprocessed sentence data (starting
from step 2). If our input is raw text articles, we also need to run
natural language processing in order to extract candidate pairs and
features. If you want to learn how NLP extraction can be done in
DeepDive (starting from step 1), you can refer to the appendix: [Data preprocessing using NLP extractor in DeepDive](walkthrough-extras.html#nlp_extractor).

The full application is also available in the folder `DEEPDIVE_HOME/examples/spouse_example`, which contains all possible implementations in [different types of extractors](extractors.html). In this document, we only introduce the default extractor type (json_extractor), which correspond to `DEEPDIVE_HOME/examples/spouse_example/default_extractor` in your DeepDive directory.


## Contents

* [Preparation](#preparation):

  - [Installing DeepDive](#installing)  
  - [Creating a new DeepDive application](#newapp)

* [Implement the data flow](#implement_dataflow):

  1. [Data preprocessing](#loading_data)
  2. [Feature extraction](#feature_extraction)
    - [Adding a people extractor](#people_extractor)
    - [Extracting candidate relations](#candidate_relations)
    - [Adding features for candidate relations](#candidate_relation_features)
  3. [Writing inference rules for factor graph generation](#inference_rules)
  4. [Statistical inference and learning](#learning_inference)
  5. [Get and check results](#get_result)

Other sections:

- [How to examine / improve results](walkthrough-improve.html)
- [Extras: preprocessing, NLP, pipelines, debugging extractor](walkthrough-extras.html)
- [Go back to the Walkthrough](walkthrough.html)

## Preparation

<a id="installing" href="#"> </a>

### Installing DeepDive

Start by [downloading and installing DeepDive on your machine](installation.html). In the rest of this tutorial, you should have a `deepdive` directory as your working directory (you cannot rename the folder to other names). Assume the path to your `deepdive` directory is `$DEEPDIVE_HOME`.

We will be using PostgreSQL as our primary database in this example. If you followed the DeepDive installation guide and passed all tests then your PostgreSQL server should be running already.

<!-- <a id="setup" href="#"> </a>

### Setting up the database

We will be using PostgreSQL as our primary database in this example. If you followed the DeepDive installation guide and passed all tests then your PostgreSQL server should be running already. Let's start by creating a new database called `deepdive_spouse` by typing in command line:

{% highlight bash %}
createdb deepdive_spouse
{% endhighlight %}

 -->

<div id="newapp" href="#"> </div>

### Creating a new DeepDive application

Start by creating a new folder `app/spouse` in the DeepDive directory for your application. *Assume your DeepDive directory is `$DEEPDIVE_HOME`*.

{% highlight bash %}
cd $DEEPDIVE_HOME
mkdir -p app/spouse   # make folders recursively
cd app/spouse
{% endhighlight %}

DeepDive's main entry point is a file called `application.conf` which contains database connection information as well as your feature extraction and inference rule pipelines. It is often useful to have a small `run.sh` script that loads environment variables and executes the DeepDive pipeline. We provide simple templates for both of these to copy and modify. Copy these templates to our directory by the following commands: 

<!-- TODO what is env.sh doing? -->

{% highlight bash %}
cp ../../examples/template/application.conf .
cp ../../examples/template/run.sh .
cp ../../examples/template/env.sh .
{% endhighlight %}

The `env.sh` file configures environment variables that will be used in this application. There is a placeholder line `DBNAME=` in that file, modify `env.sh` and fill it with your database name:
  
{% highlight bash %}
# File: env.sh
...
export DBNAME=deepdive_spouse   # modify this line
...
{% endhighlight %}

Note that in the `run.sh` file it defines environment variables `APP_HOME` which is our current directory `app/spouse`, and `DEEPDIVE_HOME` which is the home directory of DeepDive. We will use these variables later.

You can now try executing the `run.sh` file:

{% highlight bash %}
./run.sh
{% endhighlight %}

Because you have not defined any extractors or inference rules you will not see meaningful results, but DeepDive should run successfully from end to end and you should be able to see a summary report such as:

    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  Summary Report
    15:57:55 [profiler] INFO  --------------------------------------------------

<a id="implement_dataflow" href="#"> </a>

## Implement the Data Flow

Now let's start implementing the [data flow](#dataflow) for this KBC application.

<a id="loading_data" href="#"> </a>

### Data preprocessing

In this example we will be using raw text from a couple of New York Times articles. Note that there is nothing special about our data set, and you are free to use whatever raw text data you want. Let's copy the data into our directory, and load it into the database. 

We have prepared everything to free developers from data preprocessing. Copy prepared data and scripts from DeepDive `DEEPDIVE_HOME/example/spouse_example/` folder into `DEEPDIVE_HOME/app/spouse/`, by typing:

{% highlight bash %}
cp -r ../../examples/spouse_example/data .
cp ../../examples/spouse_example/schema.sql .
cp ../../examples/spouse_example/setup_database.sh .
{% endhighlight %}

Then, execute the script `setup_database.sh`, which will get all the preprocessed data ready:

{% highlight bash %}
sh setup_database.sh deepdive_spouse
{% endhighlight %}

Now following relations are provided in the database dump:

                         List of relations
     Schema |        Name         | Type  |  Owner   | Storage
    --------+---------------------+-------+----------+---------
     public | articles            | table | deepdive | heap
     public | has_spouse          | table | deepdive | heap
     public | has_spouse_features | table | deepdive | heap
     public | people_mentions     | table | deepdive | heap
     public | sentences           | table | deepdive | heap
    (5 rows)

Among these relations, `articles` contains article data and `sentences` contains parsed sentence data. Other tables are currently empty, and during feature extraction step they will be filled up.

For a detailed reference of how these tables are created and loaded, refer to [Preparing Data Tables](walkthrough-extras.html#data_tables) in the extra section.


<a id="feature_extraction" href="#"> </a>

### Feature Extraction

<a id="people_extractor" href="#"> </a>

#### Adding a people extractor

Our next task is to write several [extractors](extractors.html) in
DeepDive for feature extraction. On a high
level, each extractor performs a user-defined function (UDF) on an input
query against database, **in a row-wise manner**. One may think of an
extractor as a function which *maps one input tuple (one row in input
SQL query)* to one or more output tuples, similar to a `map` or
`flatMap` function in functional programming languages (or "Map" in
MapReduce).

Our first extractor will extract people mentions from the sentences, and put them into a new table. 
Note that we have named entity tags in column `ner_tags` of our `sentences` table. We will use this column to identify people mentions: we assume that *a word phrase is a people mention if all its words are tagged as `PERSON` in its `ner_tags` field.*

<!-- Ideally you would want to add your own domain-specific features to extract mentions. For example, people names are usually capitalized, tagged with a noun phrase part of speech tag, and have certain dependency paths to other words in the sentence. However, because the Stanford NLP Parser is relatively good at identifying people and tags them with a `PERSON` named-entity tag we trust its output and don't make the predictions ourselves. We simply assume that all people identified by the NLP Parser are correct. Note that this assumption is not ideal and usually does not work for other types of entities, but it is good enough to build a first version of our application.
 -->

<!-- Again, we first create a new table in the database by typing:
  
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
{% endhighlight %} -->

Let's create our first extractor in DeepDive, by adding several lines into `deepdive.extraction.extractors` block in `application.conf`, which should be present in the template:

    deepdive {
      ...
      # Put your extractors here
      extraction.extractors {
        
        # Extractor to extract people mentions:
        ext_people {

          # An input to the extractor is a row (tuple) of the following query:
          input           : """
                            SELECT  sentence_id, words, ner_tags
                            FROM    sentences
                            """

          # output of extractor will be written to this table:
          output_relation : "people_mentions"

          # This user-defined function will be performed on each row (tuple) of input query:
          udf             : ${APP_HOME}"/udf/ext_people.py"

          # This script will be executed once, before the extractor runs:
          before          : ${APP_HOME}"/udf/clear_table.sh people_mentions"

          # This script will be executed once, after the extractor runs:
          after           : ${APP_HOME}"/udf/fill_sequence.sh people_mentions mention_id"
        }
        # ... (more extractors to add here)
      } 
      ...   
    }

Let's go through each line:

1. The input to the `ext_people` extractor are all sentences (identifiers, words and named entity tags), selected using a SQL statement.
2. The output of the extractor will be written to the `people_mentions` table. (for the table format, refer to the [cheat-sheet](#table_cheatsheet) in appendix.)
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

# Sample input data:
'''
{"sentence_id":46494,"words":["Other","than","the","president",",","who","gets","this","honor","?"],"ner_tags":["O","O","O","O","O","O","O","O","O","O"]}
{"sentence_id":46495,"words":["When","and","how","did","it","start","?"],"ner_tags":["O","O","O","O","O","O","O"]}
{"sentence_id":45953,"words":[",","''","an","uncredited","Burton","turned","up","in","a","crowded","bar","scene","."],"ner_tags":["O","O","O","O","PERSON","O","O","O","O","O","O","O","O"]}
'''

import json, sys

# For-loop for each row in the input query
for row in sys.stdin:
  # load JSON format of the current tuple
  sentence_obj = json.loads(row)
  # Find phrases that are continuous words tagged with PERSON.
  phrases = []                          # Store (start_position, length, text)
  words = sentence_obj["words"]         # a list of all words
  ner_tags = sentence_obj["ner_tags"]   # ner_tags for each word
  start_index = 0
  
  while start_index < len(words):
    # Checking if there is a PERSON phrase starting from start_index
    index = start_index
    while index < len(words) and ner_tags[index] == "PERSON": 
      index += 1
    if index != start_index:   # found a person from "start_index" to "index"
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

If you want to get a sample of the inputs to th extractor, refer to [getting example inputs](walkthrough-extras.html#debug_extractors) section in the Extras.

This `udf/ext_people.py` Python script takes sentences records as an input, and outputs a people record for each (potentially multi-word) person phrase found in the sentence, which are one or multiple continuous words tagged with `PERSON`.

Note that if you wanted to add debug output, you can print to *stderr* instead of stdout, and the messages would appear on the terminal, as well as in the log file (`$DEEPDIVE_HOME/log/DATE_TIME.txt`).

At this point you may be wondering about the `before` and `after` script. Why do we need them? Each time before the extractor runs we want to clear out the `people_mentions` table and remove old data; after the extractor runs we want to fill the column `mention_id` with unique values for future tables to refer. These actions are pretty common in this application, so we make two general scripts, which are also used for future extractors:

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
chmod +x udf/clear_table.sh udf/fill_sequence.sh
{% endhighlight %}

If you want, you can now run your extractor by executing `./run.sh`, and you will be able to see extracted results in `people_mentions` table.

{% highlight bash %}
./run.sh    # Run extractors now
psql -d deepdive_spouse -c "select * from people_mentions limit 5;"
{% endhighlight %}

Results may look like:

     sentence_id | start_position | length |      text       | mention_id
    -------------+----------------+--------+-----------------+------------
           44348 |              5 |      1 | Bush            |          5
           44348 |             15 |      1 | Balyan          |         16
           43849 |              3 |      1 | Gilbert         |         22
           44220 |              0 |      2 | Tony Rodham     |         29
           44220 |             19 |      2 | Hillary Clinton |         37


<a id="candidate_relations" href="#"> </a>

#### Extracting candidate relations between mention pairs

Now comes the interesting part! We have laid all the groundwork to extract the `has_spouse` relation we care about. The empty table `has_spouse` is for this relation. 

To understand how DeepDive works, we should notice that the `has_spouse` table has following format:

         Table "public.has_spouse"
       Column    |  Type   | Modifiers
    -------------+---------+-----------
     person1_id  | bigint  |            # first person's mention_id in people_mentions
     person2_id  | bigint  |            # second person's mention_id
     sentence_id | bigint  |            # which senence it appears
     description | text    |            # a description of this relation pair
     is_true     | boolean |            # whether this relation is correct
     relation_id | bigint  |            # unique identifier for has_spouse
     id          | bigint  |            # reserved for DeepDive


Note that in table `has_spouse`, special `is_true` column in the above table. We need this column because we want DeepDive to predict how likely it is that a given entry in the table is correct. In other words, DeepDive will create a [random variable](general/inference.html) for each instance of it. More concretely, each row in the `has_spouse` table will be assigned random variable for its `is_true` column. 

Also note that we must reserve another special column, `id bigint`, in any table containing variables like this one. For system to use, this column should be **left blank, and not be used anywhere**. We will further see syntax requirements in *inference rules* related to this `id` column.


Let's tell DeepDive to use the `is_true` column of table `has_spouse` for probabilistic inference in the `application.conf`:

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

        # Run this extractor after "ext_people"
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

# Sample input data (piped into STDIN):
'''
{"p1.text":"Kaplan","p1.mention_id":84,"p2.text":"Charles Foster Kane","p2.mention_id":62,"sentence_id":44314}
{"p1.text":"Charles Foster Kane","p1.mention_id":62,"p2.text":"Kaplan","p2.mention_id":84,"sentence_id":44314}
{"p1.text":"Robinson","p1.mention_id":575,"p2.text":"Barack Obama","p2.mention_id":549,"sentence_id":45498}
'''

import json, csv, os, sys
BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
spouses = {}
with open (BASE_DIR + "/../../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    name1 = line[0].strip().lower()
    name2 = line[1].strip().lower()
    spouses[name1] = name2
    spouses[name2] = name1

# Load relations of people that are not spouse
non_spouses = set()
lines = open(BASE_DIR + '/../../data/non-spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair
  non_spouses.add((name2, name1))

# For each input tuple
for row in sys.stdin:
  obj = json.loads(row)
  p1_text = obj["p1.text"].strip()
  p2_text = obj["p2.text"].strip()
  p1_lower = p1_text.lower()
  p2_lower = p2_text.lower()

  is_true = None
  if p1_lower in spouses and spouses[p1_lower] == p2_lower: 
    is_true = True    # the mention pair is in our supervision dictionary
  elif (p1_lower, p2_lower) in non_spouses: 
    is_true = False   # they appear in other relations
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = False   # they are the same person

  print json.dumps({"person1_id": obj["p1.mention_id"],  "person2_id": obj["p2.mention_id"],
    "sentence_id": obj["sentence_id"],  "description": "%s-%s" %(p1_text, p2_text),
    "is_true": is_true,  "relation_id": None,  "id": None
  })
{% endhighlight %}


Now if you like, you can run the system by executing `run.sh` and check the output relation `has_spouse`. `run.sh` will run the full pipeline with both extractors `ext_people` and `ext_has_spouse`. If you only want to run your new extractor, refer to the [Pipeline section in Extras](walkthrough-extras.html#pipelines).

{% highlight bash %}
./run.sh
psql -d deepdive_spouse -c "select * from has_spouse limit 5;"
{% endhighlight %}

Results look like:

     person1_id | person2_id | sentence_id |         description         | is_true | relation_id |  id
    ------------+------------+-------------+-----------------------------+---------+-------------+-------
            717 |        745 |       45057 | Arthur-Carrie               |         |           3 | 
            717 |        731 |       45057 | Arthur-Jerry Stiller        |         |          10 | 
            773 |        759 |       45062 | Doug-Victor Williams        |         |          18 | 
           3635 |       3649 |       48432 | Charles Eskew-James Gardner |         |          24 | 
           3635 |       3621 |       48432 | Charles Eskew-Jewell Eskew  |         |          31 | 

<a id="candidate_relation_features" href="#"> </a>

#### Adding Features for candidate relations

For DeepDive to make predictions, we need to add *features* to our candidate relations. Features are properties that help decide whether or not the given relation is correct. 

Currently we use features include: (1) a bag of words between the two mentions; (2) the number of words between two phases; (3) whether the last word of two person's names (last name) are the same.
We will refine these features later in the section of [improve the results](#improve).

<!-- Create a table by typing:

{% highlight bash %}
psql -d deepdive_spouse -c "
  CREATE TABLE has_spouse_features(
    relation_id bigint,
    feature     text
  );
"
{% endhighlight %}
 -->

Create a new extractor for features, which will execute after our last `ext_has_spouse_candidates` extractor:

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
        before          : ${APP_HOME}"/udf/clear_table.sh has_spouse_features"
        dependencies    : ["ext_has_spouse_candidates"]
      }

    }


To create our extractor UDF, we will make use of `ddlib`, our python library that provides useful utilities such as `Span` to manipulate elements in sentences. 

To use `ddlib`, you should have `$DEEPDIVE_HOME/ddlib` added to your PATH or PYTHONPATH. Add following lines into `env.sh`, if it's not already there:

{% highlight bash %}
# File: env.sh
...
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH
...
{% endhighlight %}


Create script `udf/ext_has_spouse_features.py` as follows:


{% highlight python %}
#! /usr/bin/env python
# File: udf/ext_has_spouse_features.py

# Sample input data (piped into STDIN):
'''
{"p2.length":2,"p1.length":2,"words":["The","strange","case","of","the","death","of","'50s","TV","Superman","George","Reeves","is","deconstructed","in","``","Hollywoodland",",","''","starring","Adrien","Brody",",","Diane","Lane",",","Ben","Affleck","and","Bob","Hoskins","."],"relation_id":12190,"p1.start_position":20,"p2.start_position":10}
{"p2.length":2,"p1.length":2,"words":["Political","coverage","has","not","been","the","same","since","The","National","Enquirer","published","photographs","of","Donna","Rice","in","the","former","Sen.","Gary","Hart","'s","lap","20","years","ago","."],"relation_id":34885,"p1.start_position":14,"p2.start_position":20}
'''

import sys, json
import ddlib     # DeepDive python utility

# For each input tuple
for row in sys.stdin:
  obj = json.loads(row)
  words = obj["words"]
  # Unpack input into tuples.
  span1 = ddlib.Span(begin_word_id=obj['p1.start_position'], length=obj['p1.length'])
  span2 = ddlib.Span(begin_word_id=obj['p2.start_position'], length=obj['p2.length'])

  # Features for this pair come in here
  features = set()
  
  # Feature 1: Bag of words between the two phrases
  words_between = ddlib.tokens_between_spans(words, span1, span2)
  for word in words_between.elements:
    features.add("word_between=" + word)

  # Feature 2: Number of words between the two phrases
  features.add("num_words_between=%s" % len(words_between.elements))

  # Feature 3: Does the last word (last name) match?
  last_word_left = ddlib.materialize_span(words, span1)[-1]
  last_word_right = ddlib.materialize_span(words, span2)[-1]
  if (last_word_left == last_word_right):
    features.add("potential_last_name_match")

  # # Use this line if you want to print out all features extracted:
  # ddlib.log(features)

  for feature in features:  
    print json.dumps({
      "relation_id": obj["relation_id"],
      "feature": feature
    })
{% endhighlight %}


Same as before, you can run the system by executing `run.sh` and check the output relation `has_spouse_features`. (If you only want to run your new extractor, refer to the [Pipeline section in Extras](walkthrough-extras.html#pipelines).)


{% highlight bash %}
./run.sh
psql -d deepdive_spouse -c "select * from has_spouse_features limit 5;"
{% endhighlight %}

Results look like:

     relation_id |         feature
    -------------+-------------------------
           32523 | word_between=Republican
           32523 | word_between=attend
           32523 | word_between=a
           32523 | word_between=check
           32523 | word_between=him
    (5 rows)

<a id="inference_rules" href="#"> </a>

### Writing inference rules

Now we need to tell DeepDive how to generate [factor graphs](general/inference.html) to perform  perform probabilistic inference.  We want to predict the `is_true` column of the `has_spouse` table based on the features we have extracted. This is the simplest rule you can write, because it does not involve domain knowledge or relationships among variables. 

Add the following lines to your `application.conf`, into `inference.factors` block:

    # Put your inference rules here
    inference.factors {

      # A simple logistic regression rule
      f_has_spouse_features {

        # input to the inference rule is all the has_spouse candidate relations,
        #   as well as the features connected to them:
        input_query: """
          SELECT has_spouse.id      AS "has_spouse.id",
                 has_spouse.is_true AS "has_spouse.is_true",
                 feature
          FROM has_spouse,
               has_spouse_features
          WHERE has_spouse_features.relation_id = has_spouse.relation_id
          """

        # Factor function:
        function : "IsTrue(has_spouse.is_true)"

        # Weight of the factor is decided by the value of "feature" column in input query
        weight   : "?(feature)"
      }

      # ... (other inference rules)
    }

This rule generates a model similar to a logistic regression classifier. We use a set of features to make a prediction about the variable we care about. For each row in the *input query* we are adding a [factor](general/inference.html) that connects to the `has_spouse.is_true` variable with a different weight for each feature name. 

Note that the syntax requires users to **explicitly select** in `input_query`:

1. `id` column for each variable
2. The variable column, which is `is_true` in this case
3. The column weight is dependent on, which is `feature` in this case

And when selecting them, users must explicitly alias `id` to `[relation_name].id` and `[variable]` to `[relation_name].[variable]` for system to use. See more in [inference rule guide](inference_rules.html).



<!-- Finally, there is one last technical details we need to pay attention to. Our first rule is very sparse, in other words, the same features usually only applies to a few candidates that we extract. Our second rule on the other hand applies to every candidate, which means that DeepDive will learn a very high weight for it. This discrepancy can lead to longer convergence times during the inference step if we don't select a correct "learning rate". For such a case, let's change the learning rate from the default of "0.1" to the "0.001" by adding the following sampler options to the configuration file:

    sampler.sampler_args: "-l 125 -s 1 -i 200 --alpha 0.001"
 -->


<a id="learning_inference" href="#"> </a>

### Statistical inference and learning

Now that we have inference rules specified, DeepDive will automatically ground the factor graph with these rules, then perform learning to figure out the weights, and do inference to compute probabilities of candidate relations.

We are almost ready to run! In order to evaluate our results, we also want to define a *holdout fraction* for our predictions. The holdout fraction defines how much of our training data we want to treat as testing data used to compare our predictions against. By default the holdout fraction is `0`, which means that we cannot evaluate the precision of our results. Add the following line to holdout 1/4 of the training data. Modify `application.conf` with a holdout fraction:

    # Specify a holdout fraction
    calibration.holdout_fraction: 0.25


<a id="get_result" href="#"> </a>

### Get and Check results

Let's try running the full pipeline using `./run.sh`. (If you want to run part of the pipeline, refer to [pipeline section](walkthrough-extras.html#pipelines))

{% highlight bash %}
./run.sh
{% endhighlight %}


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


Great, let's take a look at some of the predictions that DeepDive has made. DeepDive creates a view `has_spouse_is_true_inference` for each variable you have defined in the database. Type in following query in command line to sample some high-confidence mention-level relations and the sentences they come from:

{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT s.sentence_id, description, is_true, expectation, s.sentence
  FROM has_spouse_is_true_inference hsi, sentences s
  WHERE s.sentence_id = hsi.sentence_id and expectation > 0.9
  ORDER BY random() LIMIT 5;
"
{% endhighlight %}


The result should be something like (might not be the same):

     sentence_id |               description                | is_true | expectation | sentence

    -------------+------------------------------------------+---------+-------------+--------------------
           26134 | Morton-Vlad                              |         |           1 | WITH THE VOICES OF : Jim Carrey -LRB- Horton -RRB- , Steve Carell -LRB- Mayor -RRB- , Carol Burnett -LRB- Kangaroo -RRB- , Will Arnett -LRB- Vlad -RRB- , Isla Fisher -LRB- Dr. Mary Lou LaRue -RRB- , Amy Poehler -LRB- Sally O'Malley -R
    RB- , Seth Rogen -LRB- Morton -RRB- , Jonah Hill -LRB- Tommy -RRB- and Dan Fogler -LRB- Councilman -RRB- .
           17871 | Ashley-Chloe                             |         |        0.95 | Developing MOVIES-OVER-DEAD-BODY-REV -- Devastated when his fianc?e Kate -LRB- Eva Longoria Parker -RRB- is killed on their wedding day , Henry -LRB- Paul Rudd -RRB- reluctantly agrees to consult a psychic named Ashley -LRB- Lake Bell
     -RRB- at the urging of his sister Chloe -LRB- Lindsay Sloane -RRB- .
           39543 | Hillary Rodham Clinton-Bill Clinton      | t       |       0.982 | Yet the Democratic establishment here is still oriented around former President Bill Clinton and his wife , Sen. Hillary Rodham Clinton .
           30399 | Thom Browne-Ashley Olsen-Lance Armstrong |         |       0.966 | HIT Thom Browne for Brooks Brothers A pairing as unlikely as the short-lived Ashley Olsen-Lance Armstrong romance , the collaboration between Thom Browne and navy-blazer supplier Brooks Brothers had the potential to be as appealing as
     a double feature of `` License to Wed '' and `` Alvin and the Chipmunks . ''
           21575 | Lauren-Conrad                            |         |       0.934 | `` I was actually with Lauren -LRB- Conrad -RRB- last Valentine 's Day and night , '' admitted the ringless Montag .
    (5 rows)


<!-- The result should be something like (might not be the same):

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
 -->

We might see that the results are actually pretty bad: most predictions are wrongly using the word "and" as the feature to indicate spouse relationship.

<!-- can see that some of these tuples are actually correct instances of married people. (Among above results, row 1, 2, 7, 8, 10 are correct) -->

In the next section, we will discuss several ways to examine and improve the results.


[Improving the results](walkthrough-improve.html)
