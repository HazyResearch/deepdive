---
layout: default
---

# Example Application Walkthrough


### Introduction

A typical use case for Deepdive is [Relation Extraction](/doc/general/relation_extraction.html). This tutorial will walk you through building a full DeepDive application that extracts `has_spouse` relationships from raw text. We use news articles as our input data and want to extract all pairs of people that participate in a `has_spouse` relation. For example *Barack Obama* and *Michelle Obama*. One can imagine how this example can be translated to other domains, such as extracting intractions between drugs, or relationships among companies.

The full application is also available in the folder `/examples/spouse_example` which contains all possible implementations in [different types of extractors](/doc/extractors.html) for this example. In this document, we only introduce the default extractor type (json_extractor).

### Contents


1. [Installing Deepdive](#installing)
2. [Setting up the database](#setup)
3. [Creating a new DeepDive application](#newapp)
4. [High-level picture of the application](#high_level_picture)
5. [Loading initial data](#loading_data)
6. [Adding a NLP extractor](#nlp_extractor)
7. [Adding a people extractor](#people_extractor)
8. [Using pipelines](#pipelines)
9. [Extracing candidate relations](#candidate_relations)
10. [Adding Features for candidate relations](#candidate_relation_features)
11. [Adding domain-specific inference rules](#inference_rules)
12. [Evaluating the result](#evaluation)



<a id="installing" href="#"> </a>

### Installing Deepdive

Start by [downloading and installing Deepdive on your machine](/doc/installation.html). In the rest of this tutorial we will assume that you have a `deepdive` directory that contais the system.


<a id="setup" href="#"> </a>

### Setting up the database

We will be using PostgreSQL as our primary database in this example. If you followed the DeepDive installation guide and passed all tests then your postgresql server should be running already. Let's start by creating a new database called `deepdive_spouse`:

{% highlight bash %}
createdb deepdive_spouse
{% endhighlight %}

<div id="#newapp" href="#"> </div>

### Creating a new DeepDive application

Start by creating a new folder in the DeepDive directory for your application. Let's called it `spouse_example`.

{% highlight bash %}
cd deepdive
mkdir -p app/spouse_example
cd app/spouse_example
{% endhighlight %}

DeepDive's main entry point is a file called `application.conf` which contains database connection information as well as your feature extraction and inference rule pipelines. It is often useful to have a small `run.sh` script that loads environment variables and executes the Deepdive pipeline. We provide simple templates for both of these to copy and modify: 


{% highlight bash %}
cp ../../examples/template/application.conf application.conf
cp ../../examples/template/run.sh run.sh
cp ../../examples/template/env.sh env.sh
{% endhighlight %}

Starty modifying the `env.sh` file with your database name:
  
{% highlight bash %}
export DBNAME=deepdive_spouse
{% endhighlight %}

You can now try executing the `run.sh` file. Because you have not defined any extractors or inference rules you will not see meaningful results, but DeepDive should run successfully from end to end and you should be able to see a summary report such as:

    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  Summary Report
    15:57:55 [profiler] INFO  --------------------------------------------------

<a id="high_level_picture" href="#"> </a>

### High-level picture of the application

In order to extract `has_spouse` relations from our text we must first identify people in the next. To extract features that help us with this task we also need to run natural language processing on our input data. On a high level, the application we want to build should:

1. Perform natural language processing on raw text
2. Extract mentions of people in the text
3. Extract all pairs of people that possibly participate in a `has_spouse` relation
4. Add features to our has_spouse candidates to predict which ones are correct or incorrect
5. Write inference rules to incorporate domain knowledge that improves our predictions

Our goal in this tutorial is get an initial application up and running. There are a couple of problems with the approach above which are worth drawing attention to: If two separate sentences mention the fact that Barack Obama and Michelle Obama are in a `has_spouse` relationship, then our approach does not know that they refer to the same fact. In other words, we ignore the fact that "Barack Obama" and "Michelle Obama" in both of these sentence refer to the same entity in the real world. We also don't recognize *coreference* of two mentions. That is, we don't know that "Barack Obama" and "Obama" probably refer to the same person. We will address these issues in the [advanced part of the tutorial](/doc/walkthrough2.html).


<a id="loading_data" href="#"> </a>

### Loading initial data

In this example we will be using raw text from a couple of New York Times articles. Note that there is nothing special about our data set, and you are free to use whatever raw text data you want. Let's copy the data into our directory and create anmd load it into the database.

{% highlight bash %}
psql -d deepdive_spouse -c "CREATE TABLE articles(
  article_id bigint,
  text text
);"
{% endhighlight %}


{% highlight bash %}
cp -r ../../examples/spouse_example/data data
psql -d deepdive_spouse -c "copy articles from STDIN CSV;" < data/articles_dump.csv
{% endhighlight %}


<a id="nlp_extractor" href="#"> </a>

### Adding an NLP extractor

The first step towards performing Entity and Relation Extraction is to extract natural language features from the raw text. This is usually done using an NLP library such as [the Stanford Parser](http://nlp.stanford.edu/software/lex-parser.shtml) or [NLTK](http://nltk.org/). Because natural language processing is such a common first step we provide a pre-built extractor which uses the [Stanford CoreNLP Kit](http://nlp.stanford.edu/software/corenlp.shtml) to split documents into sentences and tag them. Let's copy it into our project.
  
    mkdir udf
    cd ../../examples/nlp_extractor
    sbt stage

The `sbt stage` command compiles the extractor (written in Scala) and generates a handy start script. The extractor itself takes JSON tuples of raw document text as input, and outputs JSON tuples of sentences with information such as part-of-speech tags and dependency parses. Let's now create a new table for the output of the extractor in our database. Beause the output format of the NLP extractor is fixed by us, you must create a compatible table, such as:

{% highlight bash %}  
psql -d deepdive_spouse -c """
CREATE TABLE sentences(
  document_id bigint,
  sentence text, 
  words text[],
  lemma text[],
  pos_tags text[],
  dependencies text[],
  ner_tags text[],
  sentence_id bigint -- unique identifier for sentences
  );
  """
{% endhighlight %}


Next, let's tell DeepDive to use the extractor, by adding the following lines between the `deepdive {` and `}` lines in the `application.conf` file:

    extraction.extractors {
      # nlp_extractor only supports the default extractor.
      ext_sentences: {
        input: """
          SELECT  article_id, text 
          FROM    articles 
          ORDER BY article_id ASC"""
        output_relation : "sentences"
        udf             : ${DEEPDIVE_HOME}"/examples/nlp_extractor/run.sh -k article_id -v text -l 20 -t 4"
        before          : ${APP_HOME}"/udf/before_sentences.sh"
        after           : ${DEEPDIVE_HOME}"/util/fill_sequence.sh sentences sentence_id"
      }

      # ... More extractors to add dere
    }

Let's go through each line:

  1. The input to the `ext_sentences` extractor are all articles, selected using a SQL statement.
  2. The output of the extractor will be written to the `sentences` table.
  3. The extractor script is `udf/nlp_extractor/run.sh`. DeepDive will execute this command and stream input to the *stdin* of the process, and rea
  d output from *stdout* of the process. We give two command line arguments to the extractor which specifify the key and the value of the input JSON and the maximum sentence length. These are used by the NLP extractor and are not a function of DeepDive.
  4. We execute a script before the extractor runs.

There are other options you can give to extractor, refer to the [extractor guide](/doc/extractors.html) for a more comprehensive list. At this point you may be wondering about the `before` script. Why do we need that? Each time before the extractor runs we want to clear out the `sentences` table and remove old data, so let's create a `udf/before_sentences.sh`  script that does that:

{% highlight bash %}
#! /usr/bin/env bash
psql -c "TRUNCATE sentences CASCADE;" deepdive_spouse
{% endhighlight %}

Great, our first extractor is ready! When you execute `run.sh` DeepDive should run the new extractor and populate the `sentences` table with the result. Note that natural language processing is quite CPU intensive and may take a while to run. On a 2013 Macbook Pro the NLP extractor needed 1 hour to process all of the raw text documents. You can speed up this process by working with a smaller subset of the documents and using `"""SELECT * FROM articles ORDER BY id ASC LIMIT 100"""` as the input query to the extractor. Alternatively, you can also load the finished NLP result into the database directly. We provide a dump of the full `sentences` table in `data/sentences.dump`.

{% highlight bash %}
psql -d deepdive_spouse -c "copy sentences from STDIN CSV;" < ../../examples/spouse_example/data/sentences_dump.csv
{% endhighlight %}


<a id="people_extractor" href="#"> </a>

### Adding a people extractor

Our next task is to extract people mentions from the sentences. Ideally you would want to add your own domain-specific features to extract mentions. For example, people names are usually capitalized, tagged with a noun phrase part of speech tag, and have certain dependency paths to other words in the sentence. However, because the Stanford NLP Parser is relatively good at identifying people and tags them with a `PERSON` named-entity tag we trust its output and don't make the predictions ourselves. We simpy assume that all people identified by the NLP Parser are correct. Note that this assumption is not ideal and usually does not work for other types of entities, but it is good enough to build a first version of our application.

Let's write a simple extractor that puts all people mentions into their own table. This time we will write our extractor in Python. Again, we first create a new table in the database:
  
{% highlight bash %}
psql -d deepdive_spouse -c """CREATE TABLE people_mentions(
  sentence_id bigint,
  start_position int,
  length int,
  text text,
  mention_id bigint  -- unique identifier for people_mentions
  );
"""
{% endhighlight %}

As before, we also add a new extractor to our `application.conf` file:

    ext_people {
      input: """
          SELECT  sentence_id, words, ner_tags
          FROM    sentences
          """
      output_relation : "people_mentions"
      udf             : ${APP_HOME}"/udf/ext_people.py"
      before          : ${APP_HOME}"/udf/before_people.sh"
      after           : ${DEEPDIVE_HOME}"/util/fill_sequence.sh people_mentions mention_id"
      dependencies    : ["ext_sentences"]
    }

The configuration is similar to the `ext_sentences`, but note that the `ext_people` has a dependency on the `ext_sentences` extractor. This means, `ext_sentences` must be run before `ext_sentences` can be executed. Let's create the `udf/before_people.sh` script and a `udf/ext_people.py` Python script:

{% highlight bash %}  
#! /usr/bin/env bash
psql -c "TRUNCATE people_mentions CASCADE;" deepdive_spouse
{% endhighlight %}
    
{% highlight python %}
#! /usr/bin/env python

import fileinput
import json
import itertools

# For each sentence
for row in fileinput.input():
  sentence_obj = json.loads(row)

  # Find phrases that are tagged with PERSON
  phrases_indicies = []
  start_index = 0
  ner_list = list(enumerate(sentence_obj["ner_tags"]))
  while True:
    sublist = ner_list[start_index:]
    next_phrase = list(itertools.takewhile(lambda x: (x[1] in ["PERSON"]), sublist))
    if next_phrase:
      phrases_indicies.append([x[0] for x in next_phrase])
      start_index = next_phrase[-1][0] + 1
    elif start_index == len(ner_list)+1: break
    else: start_index = start_index + 1

  # Output a tuple for each PERSON phrase
  for phrase in phrases_indicies:
    print json.dumps({
      "sentence_id": sentence_obj["sentence_id"],
      "start_position": phrase[0],
      "length": len(phrase),
      "text": " ".join(sentence_obj["words"][phrase[0]:phrase[-1]+1]),
      "mention_id": None
    })
{% endhighlight %}

The `udf/ext_people.py` Python script takes sentences records as an input, and outputs a people record for each (potentially multi-word) person phrase found in the sentence. Note that if you wanted to add debug output, you can print to *stderr* instead of stdout and the messages would appear in the log file.


<a id="pipelines" href="#"> </a>

### Using pipelines

By default, DeepDive runs all extractors that are defined in the configuration file. Sometimes you only want to run some of your extractors to test them, or to save time when the output of an early extractor hasn't changed. The NLP extractor is a good example of this. It takes a long time to run, and its output will be the same every time, so we don't want to run it more than once. Deepdive allows you to deifne a [pipeline](/doc/pipelines.html) for this purpose. Add the following to your `application.conf`:

    pipeline.run: "nonlp"
    pipeline.pipelines.nonlp: ["ext_people"]

The above setting tells DeepDive to execute the "nonlp" pipeline, which only contains the "ext_people" extractor. When executing `run.sh`, your people table should be populated with the results.


<a id="candidate_relations" href="#"> </a>

### Extracting candidate relations between mention pairs

Now comes the interesting part! We have layed all the groundwork to extract the `has_spouse` relation we care about. Let's create a table for it:

{% highlight bash %}
psql -d deepdive_spouse -c """CREATE TABLE has_spouse(
  person1_id bigint,
  person2_id bigint,
  sentence_id bigint,
  description text,
  is_true boolean,
  relation_id bigint, -- unique identifier for has_spouse
  id bigint   -- reserved for DeepDive
  );
"""
{% endhighlight %}

Note the special `is_true` column in the above table. We need this column because we want DeepDive to predict how likely it is that a given entry in the table is correct. In other words, DeepDive will create a [random variable](/doc/general/inference.html) for each instance of it. More concretely, each unique id in the `has_spouse` table will be assigned random variable for its `is_true` column. Let's tell DeepDive to use the `is_true` column for probabilistic inference in the `application.conf`

    schema.variables {
      has_spouse.is_true: Boolean
    }

Let's create an extractor that extracts all candidates relations and puts them into the above table. We call them *candidate relations* because we are not sure whether or not they are actually correct, that's for Deepdive to predict. We will be adding *features* to make predictions in the next step, for now we are just outputting all of the candidates.

    ext_has_spouse_candidates {
      input: """
        SELECT  sentences.sentence_id, 
                p1.mention_id AS "p1.mention_id", 
                p1.text AS "p1.text", 
                p2.mention_id AS "p2.mention_id", 
                p2.text AS "p2.text" 
         FROM   people_mentions p1, 
                people_mentions p2, 
                sentences 
        WHERE   p1.sentence_id = p2.sentence_id 
          AND   p1.sentence_id = sentences.sentence_id 
          AND   p1.mention_id != p2.mention_id;
          """
      output_relation : "has_spouse"
      udf             : ${APP_HOME}"/udf/ext_has_spouse.py"
      before          : ${APP_HOME}"/udf/before_has_spouse.sh"
      after           : ${DEEPDIVE_HOME}"/util/fill_sequence.sh has_spouse relation_id"
      dependencies    : ["ext_people"]
    }

Here we select all pairs of people mentions that occur in the same sentence, together with the sentence itself. It may seem like we could skip writing an extractor completely and instead do this operation SQL, but there is a good reason for why want the extractor: To generate training data using [distant supervision](/doc/general/distant_supervision.html). There are some pairs of people that we know for sure are married, and we can use them as training data for DeepDive. Similarly, if we know that two people are not married, we can use them as negative training examples. In our case we will be using data from [Freebase](http://www.freebase.com/) for distant supervision. We have exported all pairs of people with a `has_spouse` relationship from the [Freebase data dump](https://developers.google.com/freebase/data) and included the CSV file in `data/spouses.csv`. 

For negative example we we will use pairs of the same person. That is, "Barack Obama" cannot be married to "Barack Obama." Note that the way we generate negative examples has an inherent bias and is less than ideal. A better approach would be to use a separate relation, such as "father" or "son" as negative evidence for a marriage relation.

Let's create a script `udf/ext_has_spouse.py` as below:

{% highlight python %}
#! /usr/bin/env python

import fileinput
import json
import csv
import os
import sys
from collections import defaultdict

BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
spouses = defaultdict(lambda: None)
with open (BASE_DIR + "/../../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    spouses[line[0].strip().lower()] = line[1].strip().lower()


# For each input tuple
for row in fileinput.input():
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_id = obj["p1.mention_id"]
  p1_text = obj["p1.text"].strip()
  p1_text_lower = p1_text.lower()
  p2_id = obj["p2.mention_id"]
  p2_text = obj["p2.text"].strip()
  p2_text_lower = p2_text.lower()
  sentence_id = obj["sentence_id"]

  # See if the combination of people is in our supervision dictionary
  # If so, set is_correct to true or false
  is_true = None
  if spouses[p1_text_lower] == p2_text_lower:
    is_true = True
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


Create a script `udf/before_has_spouse.sh` as below:

{% highlight bash %}
#! /usr/bin/env bash
psql -c "TRUNCATE has_spouse CASCADE;" deepdive_spouse
{% endhighlight %}

We also need to add our new extractor to the pipeline:

    pipeline.pipelines.nonlp: ["ext_people", "ext_has_spouse_candidates"]


<a id="candidate_relation_features" href="#"> </a>

### Adding Features for candidate relations

For DeepDive to make predictions, we need to add *features* to our candidate relations. Features are properties that help decide whether or not the given relation is correct. For example, one feature may be the sequence of words between the two mentions. We could have saved the features in the `has_spouse` table that we created above, but it is often cleaner to have a separate table for them:

{% highlight bash %}
psql -d deepdive_spouse -c """CREATE TABLE has_spouse_features(
  relation_id bigint,
  feature text);
  """
{% endhighlight %}

And our extractor:

    ext_has_spouse_features {
      input: """
        SELECT  sentences.words, 
                has_spouse.relation_id, 
                p1.start_position AS "p1.start_position", 
                p1.length AS "p1.length", 
                p2.start_position AS "p2.start_position", 
                p2.length AS "p2.length"
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

Create script `udf/ext_has_spouse_features.py` as follows:

{% highlight python %}
#! /usr/bin/env python

import fileinput
import json

# For each input tuple
for row in fileinput.input():
  obj = json.loads(row)

  # Get useful data from the JSON
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
  last_word_left = obj["words"][p1_end-1]
  last_word_right = obj["words"][p2_end-1]
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

Don't forget to add the new extractor to your pipeline:

    pipeline.pipelines.nonlp: ["ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features"]


<a id="inference_rules" href="#"> </a>

### Adding domain-specific inference rules

Now we need to tell DeepDive how to perform [probabilistic inference](/doc/general/inference.html) on the data we have generated.  We want to predict the `is_true` column of the `has_spouse` table based on the features we have extracted. This is the simplest rule you can write, because it does not involve domain knowledge or  relationships among variales. Add the following to your `application.conf`

    inference.factors {
      f_has_spouse_features {
        input_query: """
          SELECT has_spouse.id      AS "has_spouse.id",
                 has_spouse.is_true AS "has_spouse.is_true",
                 feature
          FROM has_spouse,
               has_spouse_features
          WHERE has_spouse_features.relation_id = has_spouse.id
          """
        function : "IsTrue(has_spouse.is_true)"
        weight   : "?(feature)"
      }
    }

This rule generates a model similar to a logistic regression classifier. We use a set of features to make a prediction about the variable we care about. For each row in the *input query* we are adding a [factor](/doc/general/probabilistic_inference.html) that connects to the `has_spouse.is_true` variable with a different weight for each feature name. 

The next step is to incorporate domain knowledge into our model. For example, we know that has_spouse is symmetric. That means, if Barack Obama is married to Michelle Obama, then Michelle Obama is married to Barack Obama. We can encode this knowledge in a second inference rule:

    inference.factors {
      f_has_spouse_symmetry {
        input_query: """
          SELECT r1.is_true AS "has_spouse.r1.is_true",
                 r2.is_true AS "has_spouse.r2.is_true",
                 r1.id AS "has_spouse.r1.id",
                 r2.id AS "has_spouse.r2.id"
          FROM has_spouse r1,
               has_spouse r2
          WHERE r1.person1_id = r2.person2_id
            AND r1.person2_id = r2.person1_id
          """
        function: "Imply(has_spouse.r1.is_true, has_spouse.r2.is_true)"
        weight: "?"
      }
    }


There are many [other kinds of factor functions](/doc/inference_rule_functions.html) you could use to encode domain knowledge. The final step is to add the new inference rules to our pipeline:

    pipeline.pipelines.nonlp: ["ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features", "f_has_spouse_features", "f_has_spouse_symmetry"]

In order to evaluate our results, we also want to define a *holdout fraction* for our predictions. The holdout fraction defines how much of our training data we want to treat as testing data used to compare our predictions against. By default the holdout fraction is 0, which means that we cannot evaluate the precision of our results.

    calibration.holdout_fraction: 0.25

Finally, there is one last technical details we need to pay attention to. Our first rule is very sparse, in other words, the same features usually only applies to a few candidates that we extract. Our second rule on the other hand applies to every candidate, which means that DeepDive will learn a very high weight for it. This discrepancy can lead to longer convergence times during the inference step if we don't select a correct "learning rate". For such a case, let's change the learning rate from the default of "0.1" to the "0.001" by adding the following sampler options to the configuration file:

    sampler.sampler_args: "-l 125 -s 1 -i 200 --alpha 0.001"

For more information about these options take a look at [the sampler README on github](https://github.com/dennybritz/sampler).


<a id="evaluation" href="#"> </a>

### Evaluating the result

Let's try running the full pipeline using `./run.sh`. All extractors other than the NLP extractor will run, and you should see a summary report similar to:

    09:32:13.529 [profiler][Profiler] INFO  --------------------------------------------------
    09:32:13.529 [profiler][Profiler] INFO  Summary Report
    09:32:13.530 [profiler][Profiler] INFO  --------------------------------------------------
    09:32:13.531 [profiler][Profiler] INFO  ext_people SUCCESS [24144 ms]
    09:32:13.532 [profiler][Profiler] INFO  ext_has_spouse_candidates SUCCESS [19528 ms]
    09:32:13.533 [profiler][Profiler] INFO  ext_has_spouse_features SUCCESS [22779 ms]
    09:32:13.533 [profiler][Profiler] INFO  f_has_spouse_features SUCCESS [11324 ms]
    09:32:13.533 [profiler][Profiler] INFO  f_has_spouse_symmetry SUCCESS [14280 ms]
    09:32:13.533 [profiler][Profiler] INFO  inference SUCCESS [160501 ms]
    09:32:13.534 [profiler][Profiler] INFO  calibration plot written to /YOUR/PATH/TO/deepdive/target/calibration/has_spouse.is_true.png [0 ms]
    09:32:13.534 [profiler][Profiler] INFO  calibration SUCCESS [1601 ms]
    09:32:13.534 [profiler][Profiler] INFO  --------------------------------------------------

Great, let's take a look at some of the predictions that DeepDive has made. Deepdive creates a view for each variable you have defined in the database:

{% highlight bash %}
psql -d deepdive_spouse -c "select sentence_id, description, expectation from has_spouse_is_true_inference where expectation > 0.9 limit 10;"
{% endhighlight %}

     sentence_id |               description               | expectation 
    -------------+-----------------------------------------+-------------
         3092294 | John Turturro-Lisa Pepper               |           1
         2524571 | Kris Marshall-Ewen Bremner              |           1
         5570873 | John McCain-Ethan Tucker                |           1
          837393 | Lynn Collins-Jim Carrey                 |           1
         1786337 | Catalina Sandino Moreno-Tom Tykwer      |           1
         6391166 | John Surratt-Henry Wadsworth Longfellow |           1
         4415973 | Isla Fisher-Will Arnett                 |           1
          787294 | Alison Schwartz-Kevin                   |           1
         2906528 | Fionnula Flanagan-Annabeth Gish         |           1
         1786337 | Ethan Coen-Catalina Sandino Moreno      |           1


We can see that some of these tuples are actually correct instances of married people. Others are movie actors appearing together in a movie. How can we improve these predictions? DeepDives generates [calibration plots](/doc/general/calibration.html) for all variables defined in the schema to help with debugging. Let's take a look at the geneated calibration plot, written to the file outputted in the summary report above. It should look something like this:

![Calibration](/assets/walkthrough_has_spouse_is_true.png)

The calibration plot contains useful information that help you to improve the quality of your predictions. For actionable advice about interpreeting calibration plots, refer to the [calibration guide](/doc/general/calibration.html). There are many different ways to improve the predictions above, including:

- Making use of coreference information ([see the advanced walkthorugh](/doc/walkthrough2.html))
- Performing entity linking instead of extraction relations among mentions in the text ([see the advanced walkthorugh](/doc/walkthrough2.html))
- Adding more inference rules that encode your domain knowledge
- Adding more (or better) positive or negative training examples
- Adding more (or better) features

Often, it is also useful to look at the *weights* that were learned for features or rules. You can do this by looking at the `mapped_inference_results_weights` table in the database:

{% highlight bash %}
psql -d deepdive_spouse -c "select description, weight from dd_inference_result_variables_mapped_weights limit 5;" 
{% endhighlight %}

                                             description                                         |      weight       
    ---------------------------------------------------------------------------------------------+-------------------
     f_has_spouse_symmetry()                                                                     |  70.2297702297702
     f_has_spouse_features(has_spouse_features.feature=Some(words_between=and-former-President)) |  4.10097479790514
     f_has_spouse_features(has_spouse_features.feature=Some(words_between=,-the-wife-of-Sen.))   |  3.39799946241785
     f_has_spouse_features(has_spouse_features.feature=Some(num_words_between=43))               | -3.21234959136931
     f_has_spouse_features(has_spouse_features.feature=Some(words_between=,-the-widower-of))     |  3.19385679336731


Here we can see that the word phrase "and-former-Presiden" in between the two person names has a rather high weight. This seems strange, since this phrase is not an indicator of a marriage relationship. One way to improve our predictions would be to add more negative evidence that would lower the weight of that feature.

