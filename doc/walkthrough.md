---
layout: default
---

# Example Application Walkthrough

### Introduction

A typical use case for Deepdive is that of [Relation Extraction](/doc/general/relation_extraction.html). This tutorial will walk you through building a full DeepDive application that can be used to extract `has_spouse` relationships from arbitary text. We use news articles as our input data, and hope to extract all (person, person) pairs, that participate in a `has_spouse` relation. For example *Barack Obama* and *Michelle Obama*. One can easily imagine how this example can be translated to other domain, such as extracting intractions between drugs, or relationships among companies.


### Installing Deepdive

Start by [downloading and installing Deepdive on your machine](/doc/installation.html). In the rest of this tutorial we will assume that you have a `deepdive` directory that contais the DeepDive system.

### Setting up the database

We will be using PostgreSQL as our primary database in this example. If you followed the DeepDive installation guide, your postgresql server should be ready to go. Let's start by creating a new database called `deepdive_spouse`:

    createdb deepdive_spouse

### Creating a new DeepDive application

Start by creating a new folder in the DeepDive directory for your application. Let's called it `spouse_example`.

    cd deepdive
    mkdir -p app/spouse_example
    cd app/spouse_example

DeepDive's main entry point is a file called `application.conf`, which defines database connection information as well as your feature extraction and inference rule pipelines. It is also useful to have a small `run.sh` script that loads environment variables and executes the Deepdive pipeline. We provide simple templates for both of these, which you can copy and modify: 

    cp ../../examples/template/application.conf application.conf
    cp ../../examples/template/run.sh run.sh

Starty modifying the `run.sh` file with your database name:
  
    export DBNAME=deepdive_spouse

You can now try executing the `run.sh` file. Because you have not defined any extractors or inference rules there won't be any results, but DeepDive should run successfully from end to end and you should be able to see a summary report similar to:

    12:15:18.350 [default-dispatcher-5][profiler][Profiler] INFO  Summary Report
    12:15:18.351 [default-dispatcher-5][profiler][Profiler] INFO  --------------------------------------------------
    12:15:18.351 [default-dispatcher-5][profiler][Profiler] INFO  inference SUCCESS [1993 ms]
    12:15:18.351 [default-dispatcher-5][profiler][Profiler] INFO  calibration SUCCESS [5 ms]
    12:15:18.352 [default-dispatcher-5][profiler][Profiler] INFO  --------------------------------------------------

### High-level picture of the application

TODO: A picture and high-level description comes here soon!

### Loading initial data

For this example we will be using raw text from the [Reuters news collection](http://kdd.ics.uci.edu/databases/reuters21578/reuters21578.html), a collection of 20,000 news articles. A raw data CSV file, as well as the script used the generate the CSV can be found in the `raw_data` directory. Let's create a table for the data and load it into the database.


{% highlight sql %}
CREATE TABLE articles(
  id bigserial primary key,
  text text
);
{% endhighlight %}


{% highlight bash %}
psql -d deepdive_spouse -c "copy articles(text) from STDIN CSV;" < raw_data/reuters.csv
{% endhighlight %}

### Adding a NLP extractor

The first step towards performing Relation Extraction is to extract features and mentions from the raw text. This is usually done using an NLP library such as [the Stanford Parser](http://nlp.stanford.edu/software/lex-parser.shtml) or [NLTK](http://nltk.org/). Because natural language processing is such a common first step we provide a pre-built extractor which uses the [Stanford CoreNLP Kit](http://nlp.stanford.edu/software/corenlp.shtml) to split documents into sentences and tag them. Let's copy it into our project.
  
    mkdir udf
    cp -r ../../util/nlp_extractor/ udf/
    cd udf/nlp_extractor/
    sbt stage

The `sbt stage` command compiles the extractor (written in Scala) and generates a handy start script. The extractor itself takes JSON tuples of raw document text as input, and outputs JSON tuples of sentences with information such as part-of-speech tags and dependency parses. Let's now create a new table for the output of the extractor in our database. Beause the output format of our extractor is fixed by us, you must create a compatible table, such as:

{% highlight sql %}  
CREATE TABLE sentences(
  id bigserial primary key, 
  document_id bigint,
  sentence text, 
  words text[],
  pos_tags text[],
  dependencies text[],
  ner_tags text[]);
{% endhighlight %}


Next, let's tell DeepDive to use the extractor, by adding it to the `application.conf` file:

    extraction.extractors {
      ext_sentences.input: "SELECT * FROM articles"
      ext_sentences.output_relation: "sentences"
      ext_sentences.udf: ${APP_HOME}"/udf/nlp_extractor/run.sh -k articles.id -v articles.body"
      ext_sentences.before: ${APP_HOME}"/udf/before_sentences.sh"
    }

Let's go through each line:

  1. The input to the `ext_sentences` extractor are all articles.
  2. The output of the extractor will be written to the `sentences` table.
  3. The extractor function is in `udf/nlp_extractor/run.sh`. DeepDive will execute this command and stream input and output from it using *stdin* and *stdout*. We give two command line arguments to the extractor which specifify the key and the value of the input JSON object. This is specific to the NLP extractor, not a function of DeepDive.
  4. We execute a script before the extractor runs.

There are many more options you can give to extractor, refer to the [extractor guide](/doc/extractors.html) for a comprehensive list. At this point you may be wondering about the `before` script. What is that? Before the extractor runs, we want to clear out the `sentences` table and remove old data, so let's create the `udf/before_sentences.sh`  script that does that:

{% highlight bash %}  
#! /usr/bin/env bash
echo psql -c "TRUNCATE sentences CASCADE;" $DB_NAME
{% endhighlight %}

Okay, our first extractor is ready. When you execute `run.sh` DeepDive should run the new extractor and populate the `sentences` table with the result.

### Adding a people extractor

Our next task is to extract people mentions from the sentences. Because the Stanford NLP Parser is relatively good at identifying people, and tags them with a `PERSON` named-entity tag, we trust its output and don't perform any inference on it. We simpy assume that all people identified by the NLP Parser are correct. This assumption is not ideal and may not be applicable to other domains, but will give us good enough results for a first version of our application. So, let's write a simple extractor that outputs the people mentions into their own table. This time, we write out extractor in Python. Again, we first create a new table in the database:
  
{% highlight sql %}
CREATE TABLE people(
  id bigserial primary key, 
  sentence_id bigint references sentences(id),
  start_position int,
  length int,
  text text);
{% endhighlight %}

As before, we also add a new extractor to our `application.conf` file:

    ext_people.input: "SELECT * FROM sentences"
    ext_people.output_relation: "people_mentions"
    ext_people.udf: ${APP_HOME}"/udf/ext_people.py"
    ext_people.before: ${APP_HOME}"/udf/before_people.sh"
    ext_people.dependencies: ["ext_sentences"]

The configuration is similar to the `ext_sentences`, but note that the `ext_people` has a dependency on the `ext_sentences` extractor. This means, `ext_sentences` must be completed before `ext_sentences` can run. Okay, let's now create a `udf/before_people.py` script:

{% highlight bash %}  
#! /usr/bin/env bash
echo psql -c "TRUNCATE people CASCADE;" $DB_NAME
{% endhighlight %}

And a `udf/ext_people.py` Python script:
    
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
      phrases.append([x[0] for x in next_phrase])
      start_index = next_phrase[-1][0] + 1
    elif start_index == len(ner_list)+1: break
    else: start_index = start_index + 1

  # Output a tuple for each PERSON phrase
  for phrase in phrases_indicies:
    print json.dumps({
      "sentence_id": sentence_obj["id"],
      "start_position": phrase[0],
      "length": len(phrase),
      "text": " ".join(sentence.words[phrase[0]:phrase[-1]+1])
    })
{% endhighlight %}

The `udf/ext_people.py` Python script takes sentences records as an input, and outputs a people record for each (potentially multi-word) person phrase found in the sentence. When executing `run.sh`, your people table should be populated with the results.


### Extracting candidate relations between mention pairs

Now comes the interesting part. We have layed all the groundwork to extract the `has_spouse` relation we care about. Let's create a table for it:

{% highlight sql %}
CREATE TABLE has_spouse(
  id bigserial primary key, 
  person1_id bigint references people(id),
  person2_id bigint references people(id),
  sentence_id bigint references sentences(id),
  description text,
  is_true boolean);
{% endhighlight %}

Note the special `is_true` column in the above table. We need this column because we want DeepDive to predict how likely it is that a given entry in the table is correct. In other words, DeepDive will create a [random variable](/doc/general/probabilistic_inference) for each instance of it. Let's tell DeepDive to use the `is_true` column for probabilistic inference in the `application.conf`

    schema.variables {
      has_spouse.is_true: Boolean
    }

Let's create an extractor that extracts all candidates relations and puts them into the above table. We call them *candidate relations* because we are not sure whether or not they are actually correct, that's for Deepdive to predict. We will be adding *features* used for making predictions in the next step, for now we are just outputting the candidates.

    ext_has_spouse_candidates: """SELECT * FROM people_mentions p1, people_mentions p2, sentences
      WHERE p1.sentence_id = p2.sentence_id AND p1.sentence_id = sentence.id"""
    ext_has_spouse_candidates.output_relation: "has_spouse"
    ext_has_spouse_candidates.udf: ${APP_HOME}"/udf/ext_has_spouse.py"
    ext_has_spouse_candidates.before: ${APP_HOME}"/udf/before_has_spouse.sh"
    ext_has_spouse_candidates.dependencies: ["ext_people"]

Here we select all pairs of people mentions that occur in the same sentence as well as the sentence itself. It may seem like we could skip writing an extractor completely and instead do this in SQL, but there is a good reason why want the extractor: To generate training data using [distant supervision](/doc/general/distant_supervision.html). There are some pairs of people that we know for sure are married, and we can use them as training data for DeepDive.

For this example we will be using data from [Freebase](http://www.freebase.com/) for distant supervision. We extracted all pairs of people with a `has_spouse` relationships from the [Freebase data dump](https://developers.google.com/freebase/data) and included a tab-separated list in `data/spouse_examples.txt`.

{% highlight python %}
#! /usr/bin/env python

import fileinput
import json

# For each input tuple
for row in fileinput.input():
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_id = obj["people_mentions.p1.id"]
  p1_text = obj["people_mentions.p1.text"]
  p2_id = obj["people_mentions.p2.id"]
  p2_text = obj["people_mentions.p2.text"]
  sentence_id = obj["sentences.id"]

  # See if the combination of people is in our supervision dictionary
  # If so, set is_correct to true
  is_true = None
  # TODO: No data...

  json.dumps({
    "person1_id": p1_id,
    "person2_id": p2_id,
    "sentence_id": sentence_id,
    "description": "%s-%s" %(p1_text, p2_text),
    "is_true": is_true
  })
{% endhighlight %}

{% highlight bash %}
#! /usr/bin/env bash
echo psql -c "TRUNCATE has_spouse CASCADE;" $DB_NAME
{% endhighlight %}

### Adding Features for candidate relations

Of course, for DeepDive to be able to make predictions, we need to add *features* to our candidate relations. Features are properties that help decide whether or not the given relation is correct. For example, one feature may be the sequence of words between the two mentions. We could have saved the features in the `has_spouse` table that we created above, but it is often cleaner to have a separate table for them:

{% highlight sql %}
CREATE TABLE has_spouse_features(
  id bigserial primary key, 
  relation_id bigint references has_spouse(id),
  feature text);
{% endhighlight %}

And our extractor:

    ext_has_spouse_features: """SELECT * FROM has_spouse, people_mentions p1, people_mentions p2, sentences
      WHERE has_spouse.person1_id = p1.id AND has_spouse.person2_id = p2.id 
        AND has_spouse.sentence_id = sentences.id"""
    ext_has_spouse_features.output_relation: "has_spouse_features"
    ext_has_spouse_features.udf: ${APP_HOME}"/udf/ext_has_spouse_features.py"
    ext_has_spouse_features.before: ${APP_HOME}"/udf/before_has_spouse_features.sh"
    ext_has_spouse_features.dependencies: ["ext_has_spouse_candidates"]


{% highlight python %}
#! /usr/bin/env python

import fileinput
import json

# For each input tuple
for row in fileinput.input():
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_start = obj["people_mentions.p1.start_position"]
  p1_length = obj["people_mentions.p1.length"]
  p1_end = p1_start + p1_length
  p2_start = obj["people_mentions.p2.start_position"]
  p2_length = obj["people_mentions.p2.length"]
  p2_end = p2_start + p2_length

  # Features for this pair come in here
  features = []
  
  # Feature 1: Words between the two phrases
  left_idx = min(p1_end, p2_end) + 1
  right_idx = max(p1_start, p2_start)
  word_between = obj["sentences.words"][left_idx:right_idx]
  features.append(word_between)

  for feature in features:  
    json.dumps({
      "relation_id": obj["has_spouse.id"],
      "feature": feature
    })
{% endhighlight %}


{% highlight bash %}
#! /usr/bin/env bash
echo psql -c "TRUNCATE has_spouse_features CASCADE;" $DB_NAME
{% endhighlight %}


### Adding domain-specific inference rules

Now we need to tell DeepDive how to perform [probabilistic inference](/doc/general/probabilistic_inference.html) on the data we have generated. We want to predict the `is_true` column of the `has_spouse` table based on the features we have extracted. This is the simplest rule you can write, because it does not involve any relationships among variales. Add the following to your `application.conf`

    f_has_spouse_features.input_query: """SELECT * FROM has_spouse, has_spouse_features 
      WHERE has_spouse_features.relation_id = has_spouse.id"""
    f_has_spouse_features.function: IsTrue(has_spouse.is_true)
    f_has_spouse_features.weight: ?(has_spouse_features.feature)

We also know that has_spouse is symmetric. That means, if Barack Obama is married to Michelle Obama, then Michelle Obama is married to Barack Obama. We can encode this knowledge in a second inference rule:

    f_has_spouse_features.input_query: """SELECT * FROM has_spouse r1, has_spouse r2 
      WHERE r1.person1_id = r2.person2_id AND r1.person2_id = r2.person1_id"""
    f_has_spouse_features.function: Equal(has_spouse.r1.is_true, has_spouse.r2.is_true)
    f_has_spouse_features.weight: ?


### Evaluating the result

TODO