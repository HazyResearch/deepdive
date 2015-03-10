---
layout: default
---

# Example Application: A Mention-Level Extraction System

This document describes how to **build an application to extract mention-level
marriage (`has_spouse`) relation between two people from text** in DeepDive. 

This document assumes you are familiar with basic concepts in DeepDive and in
[Knowledge Base Construction](../../general/kbc.html). Please refer to other
[documents](../../../index.html#documentation) to learn more about these topics.

## <a name="high_level_picture" href="#"> </a> High-level picture of the application

This tutorial shows how to build a full DeepDive application that extracts
**mention-level** `has_spouse` (i.e., marriage) relationships from raw text. We
use news articles as the input data and we want to extract all pairs of people
that participate in a `has_spouse` relation, for example *Barack Obama* and
*Michelle Obama*. This example can be easily translated into an application for
relation extraction in other domains, such as interactions between drugs, or
relationships among companies.

<a name="dataflow" href="#"> </a>

At a high level, we will go through the following steps:

1. Data preprocessing and loading
2. Candidate generation and Feature extraction: 
  - Extract mentions of people in the text
  - Extract all candidate pairs of people that possibly participate in a 
    `has_spouse` relation and prepare training data by 
    [distant supervision](../../general/relation_extraction.html) 
    using an existing knowledge base
  - Add features to `has_spouse` candidates
3. Generate the factor graph as specified by inference rules
4. Perform statistical learning and inference and generate the results

<!--
We will use `tsv_extractors` for our
extractors. A similar example application with implementations for the
[different types of extractors](../extractors.html) is available under
`$DEEPDIVE_HOME/examples/spouse_example`. 
-->

This tutorial assumes that you [installed DeepDive](../installation.html). The
installation directory of DeepDive is denoted as`$DEEPDIVE_HOME`. The database
system used for this tutorial is PostgreSQL. If you followed the [DeepDive
installation guide](../installation.html) and all tests completed successfully,
then your PostgreSQL server should already be running.

The full application we develop in this and in the following section of the
tutorial is also available in the directory
`$DEEPDIVE_HOME/examples/tutorial_example`. 


### Contents

* [Preparation](#preparation)
* [Implement the data flow](#implement_dataflow)
  1. [Data preprocessing](#loading_data)
  2. [Candidate generation and Feature extraction](#feature_extraction)
      - [Adding a people extractor](#people_extractor)
      - [Extracting and supervising candidate relations](#candidate_relations)
      - [Adding features to candidate relations](#candidate_relation_features)
  3. [Writing inference rules and defining holdout](#inference_rules)
  4. [Run and Get results](#get_result)

Other sections:

- [How to examine / improve results](walkthrough-improve.html)
- [Extras: preprocessing, NLP, pipelines, debugging
  extractor](walkthrough-extras.html)

## <a name="preparation" href="#"></a> Preparation

We start by creating a new application folder `app/spouse` in the DeepDive
installation directory:

```bash
cd $DEEPDIVE_HOME
mkdir -p app/spouse   # make folders recursively
cd app/spouse
```

From now on we will denote the `$DEEPDIVE_HOME/app/spouse` directory as
`APP_HOME`.

DeepDive's main entry point is the file `application.conf` which contains
all information and configuration settings needed to run an application, e.g.,
database connection information, extractor specification, inference rules,
pipelines, and so on. A template `application.conf` is in
`$DEEPDIVE_HOME/examples/template/application.conf` and must be copied 
into `$APP_HOME`:

```
cp $DEEPDIVE_HOME/examples/template/application.conf $APP_HOME
```

The execution of the application is controlled by a script `run.sh`. We created
this script `$DEEPDIVE_HOME/examples/tutorial_example/step1-basic/run.sh`
which should be copied to `$APP_HOME`:

```
cp $DEEPDIVE_HOME/examples/tutorial_example/step1-basic/run.sh $APP_HOME
```

The `run.sh` scripts contains the definitions of two environment variables:
`$DEEPDIVE_HOME` which is the installation directory of DeepDive, and `APP_HOME`
which is the directory `$DEEPDIVE_HOME/app/spouse`. We will use these variables
later. It also contains the definitions of various database connection
parameters that you should set according to your database settings. Finally, it
contains the commands to actually run the application.

In order to write the application, we need some data files, namely the input corpus of
text and some existing knowledge base of interpersonal relationship. 
**[Download the archive here](http://i.stanford.edu/hazy/deepdive-tutorial-data.zip)**.
Expand the archive in the `$APP_HOME/data` directory. Specific steps:

```bash
cd $APP_HOME
mkdir data
cd data

# Download and expand the data archive
wget http://i.stanford.edu/hazy/deepdive-tutorial-data.zip
unzip deepdive-tutorial-data.zip
```

Now your `$APP_HOME/data` directory should contain following files:

```
non-spouses.tsv  sentences_dump.csv  sentences_dump_large.csv  spouses.tsv
```

## <a name="implement_dataflow" href="#"></a> Implement the Data Flow

Let's now implement the [data flow](#dataflow) for this KBC application.

### <a name="loading_data" href="#"></a> Step 1: Data preprocessing and loading

In this tutorial, we start from preprocessed sentence data, i.e., data that has
already been parsed and tagged with a NLP toolkit. If the input corpus was
instead composed by raw text articles, we would also need to perform some
Natural Language Processing on the corpus before being able to extract candidate
relation pairs and features. To learn how NLP extraction can be done in
DeepDive, you can refer to the [appendix](walkthrough-extras.html#nlp_extractor)
of this tutorial.

We start by some scripts from the
`$DEEPDIVE_HOME/example/tutorial_example/step1-basic` folder into `APP_HOME`

```bash
cp $DEEPDIVE_HOME/examples/tutorial_example/step1-basic/schema.sql $APP_HOME
cp $DEEPDIVE_HOME/examples/tutorial_example/step1-basic/setup_database.sh $APP_HOME
```

Then, we run the script `$APP_HOME/setup_database.sh`, which creates the database and the
necessary tables and loads the data. If you are interested in what this script does, refer to the [appendix](walkthrough-extras.html#data_tables).

```bash
./setup_database.sh deepdive_spouse
```

This will create and populate some relations. You can check that the relations
have been successfully created with the following command (also, use `\q` to quit database prompt after
running the following commands):

```bash
psql deepdive_spouse
\d
```

The output should be the following:

                         List of relations
     Schema |        Name         | Type  |  Owner
    --------+---------------------+-------+----------
     public | articles            | table | deepdive
     public | has_spouse          | table | deepdive
     public | has_spouse_features | table | deepdive
     public | people_mentions     | table | deepdive
     public | sentences           | table | deepdive
    (5 rows)


- `sentences` contains processed sentence data by an [NLP
  extractor](walkthrough-extras.html#nlp_extractor). This table contains
  tokenized words, lemmatized words, Part Of Speech tags, Named Entity
  Recognition tags, and dependency paths for each sentence.
- the other tables are currently empty, and will be populated during the
  candidate generation and the feature extraction steps.

For a detailed reference of how these tables are created and loaded, refer to
the [Preparing the Data Tables](walkthrough-extras.html#data_tables) section in
the appendix.

### <a name="feature_extraction" href="#"></a> Step 2: Candidate Generation and Feature Extraction

Our next task is to write several [extractors](../extractors.html) for candidate
generation and feature extraction. 

In this step, we create three extractors whose UDFs are Python scripts. The
scripts will will go through the sentences in the corpus and, respectively:

1. create mentions of people;
2. create candidate pairs of people mentions that may be in a marriage relation,
	and supervise some of them using distant supervision rules;
3. add features to the candidates, which will be used by DeepDive to learn how
	to distinguish between correct marriage relation mentions and incorrect
	ones;

#### <a name="people_extractor" href="#"></a> Adding a people extractor

We first need to recognize the persons mentioned in a sentence. Given a
set of sentences, the person mention extractor will populate a relation
`people_mention` that contains an encoding of the mentions. In this example, we
build a simple person recognizer that simply uses the NER tags created by the
NLP toolkit. We could do something more sophisticated, but in this tutorial we
just want to illustrate the basic concepts of KBC building.

**Input:** sentences along with NER tags. Specifically, each line in the input to
this extractor UDF is a row from the `sentences` table in a special TSV (Tab
Separated Values) format, where the arrays have been transformed into strings
with elements separated by `~^~`, e.g.:

    118238@10       Sen.~^~Barack~^~Obama~^~and~^~his~^~wife~^~,~^~Michelle~^~Obama~^~,~^~have~^~released~^~eight~^~years~^~of~^~joint~^~returns~^~.        O~^~PERSON~^~PERSON~^~O~^~O~^~O~^~O~^~PERSON~^~PERSON~^~O~^~O~^~O~^~DURATION~^~DURATION~^~O~^~O~^~O~^~O

**Output:** TSV lines that can be loaded as rows of the `people_mentions` table, e.g.:

    118238@10	1	2	Barack Obama	118238@10_1
    118238@10	7	2	Michelle Obama	118238@10_7

This first extractor identifies people mentions (in the above sample ,
"Barack Obama" and "Michelle Obama") in the sentences, and insert them into the table
`people_mentions`.  We use the named entity tags from the `ner_tags` column of
the `sentences` table to identify word phrases that have all words tagged as
`PERSON`.

To define our extractors in DeepDive, we start by adding several lines
into the `deepdive.extraction.extractors` block in `application.conf`, which
should already be present in the template:

```bash
deepdive {
  ...
  # Put your extractors here
  extraction.extractors {

    # Extractor 1: Clean output tables of all extractors
    ext_clear_table {
      style: "sql_extractor"
      sql: """
        DELETE FROM people_mentions;
        DELETE FROM has_spouse;
        DELETE FROM has_spouse_features;
        """
    }
    
    # Extractor 2: extract people mentions:
    ext_people {
      # The style of the extractor
      style: "tsv_extractor"
      # An input to the extractor is a row (tuple) of the following query:
      input: """
        SELECT  sentence_id,
                array_to_string(words, '~^~'),
                array_to_string(ner_tags, '~^~')
          FROM  sentences"""

      # output of extractor will be written to this table:
      output_relation: "people_mentions"

      # This user-defined function will be performed on each row (tuple) of input query:
      udf: ${APP_HOME}"/udf/ext_people.py"

      dependencies: ["ext_clear_table"]
    }
    # ... (more extractors to add here)
  } 
  ...   
}
```

Note that we first create an extractor `ext_clear_table`, which is executed
before any other extractor and empties the output tables of all other
extractors. This is a `sql_extractor`, which is just a set of SQL
commands specified in the `sql` field.

For our person mention extractor `ext_people`, the meaning of each line is the
following:

1. The style of the extractor is a `tsv_extractor`.

2. The input to the `ext_people` extractor is the output of the given SQL query,
which selects all rows from the `sentences` table, projecting on the columns
`sentence_id`, `words`, and `ner_tags`. These last two columns are arrays that
are converted to strings with the elements separated by `~^~` (this is to ease
the parsing by the extractor, as the TSV format for arrays is not
straightforward to parse)

3. The output of the extractor will be written to the `people_mentions` table
(for the table format, refer to the [cheat-sheet](#table_cheatsheet) in the
appendix).

4. The extractor script is `udf/ext_people.py`. DeepDive launches this
command, stream rows returned by the input query to the *stdin* of the process,
reads output lines from *stdout* of the process, and loads these lines into the
`people_mentions` table.

5. The `dependencies` field specifies that this extractor can be executed only
after the `ext_clear_table` extractor has completed.

For additional information about extractors, refer to the ['Writing extractors'
guide](../extractors.html).

We then create a `udf` directory to store the scripts:

```bash
mkdir $APP_HOME/udf
```

Then we create a `udf/ext_people.py` script which acts as UDF for the people
mention extractor. The script scans input sentences and outputs phrases
representing mentions of people. The script contains the following code:

(a copy of this script is also available from
`$DEEPDIVE_HOME/examples/tutorial_example/step1-basic/udf/ext_people.py`):

```python
#! /usr/bin/env python

import sys

ARR_DELIM = '~^~'  # Array element delimiter in strings

# For-loop for each row in the input query
for row in sys.stdin:
  # Find phrases that are continuous words tagged with PERSON.
  sentence_id, words_str, ner_tags_str = row.strip().split('\t')
  words = words_str.split(ARR_DELIM)
  ner_tags = ner_tags_str.split(ARR_DELIM)
  start_index = 0
  phrases = []

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
    print '\t'.join(
      [ str(x) for x in [
        sentence_id,
        start_position,   # start_position
        length, # length
        text,  # text
        '%s_%d' % (sentence_id, start_position)        # mention_id
      ]])
```

To get a sample of the inputs to the extractor, refer to [getting example
inputs](walkthrough-extras.html#debug_extractors) section in the Extras.

This `udf/ext_people.py` Python script takes sentence records as an input, and
outputs a people mention record for each set of one or more continuous words
with NER tag `PERSON` in the sentence.

To add debug output, you can print to *stderr* instead of stdout, and the
messages would appear on the terminal, as well as in the DeepDive log file
(`$DEEPDIVE_HOME/log/DATE_TIME.txt`).

You can now run the extractor by executing `./run.sh`. Once the run has
completed, you should be able to see the extracted results in the
`people_mentions` table. We select the results of the sample input data to see
what happens in the extractor:

```bash
./run.sh    # Run extractors now
[...]
psql -d deepdive_spouse -c "select * from people_mentions where sentence_id='118238@10'"
```

The results are:

     sentence_id | start_position | length |      text      | mention_id
    -------------+----------------+--------+----------------+-------------
     118238@10   |              1 |      2 | Barack Obama   | 118238@10_1
     118238@10   |              7 |      2 | Michelle Obama | 118238@10_7
    (2 rows)


To double-check that the results you obtained are correct, count the number of
tuples in your tables:

```bash
psql -d deepdive_spouse -c "select count(*) from sentences;"
psql -d deepdive_spouse -c "select count(*) from people_mentions;"
```

The results should be:

     count
    -------
     55469

     count
    -------
     88266

#### <a name="candidate_relations" href="#"></a> Extracting candidate relations between mention pairs

Now we need to create candidates for `has_spouse` relations. For
simplicity, a relation candidate is a **pair of person mentions in the same
sentence**.

In order to train the system to decide whether a candidate is indeed expressing
a marriage relation, we also need to generate some *training data*. However, it
is hard to find ground truth on whether two mentions participate in `has_spouse`
relation. Therefore, we use [distant
supervision](../../general/distant_supervision.html) rules that generate
mention-level training data by heuristically mapping them to known entity-level
relations in an existing knowledge base.  

The extractor we are now going to write takes all mentions of people in a
sentence, and insert each pair of them into the table `has_spouse`, while also
adding labeling some of these pairs as `true` or `false` according to the
supervision rules.

**Input:** two mentions from the `people_mentions` table coming from the same
sentence, e.g.:

    118238@10	118238@10_7	Michelle Obama	118238@10_1	Barack Obama

**Output:** one row in `has_spouse` table:

    118238@10_7	118238@10_1	118238@10	Michelle Obama-Barack Obama	t	118238@10_7-118238@10_1	247956


To understand how DeepDive works, we should look at the schema of the
`has_spouse` table:

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


The table `has_spouse` contains `is_true` column. We need this column because we
want DeepDive to predict how likely it is that a given entry in the table is
correct. Each row in the `has_spouse` table will be assigned a random variable
for its `is_true` column, representing whether the corresponding relation
candidate is expressing a marriage relation or not. DeepDive will predict the
probability of this Boolean random variable to be `true`.

Also note that we must reserve another special column, `id`, of type `bigint`,
in any table containing variables. This column should be **left
blank, and not be used anywhere** in your application. We will further see
inference rule syntax requirements related to the `id` column.

We now tell DeepDive to create variables for the `is_true` column of the
`has_spouse` table for probabilistic inference, by adding the following line
to the `schema.variables` block in `application.conf`:

      schema.variables {
        has_spouse.is_true: Boolean
      }

We now define an extractor that creates all candidate relations and inserts them
into the table `has_spouse`. We call them *candidate relations* because we do
not know  whether or not they are actually expressing a marriage relation:
that's for DeepDive to predict later. Add the following to `application.conf` to
define the extractor:

```bash
  extraction.extractors {

    # ... (other extractors)

    # Extractor 3: extract mention relation candidates
    ext_has_spouse_candidates {
      # The style of the extractor
      style: tsv_extractor
      # Each input (p1, p2) is a pair of mentions
      input: """
        SELECT  sentences.sentence_id,
                p1.mention_id AS p1_mention_id,
                p1.text       AS p1_text,
                p2.mention_id AS p2_mention_id,
                p2.text       AS p2_text
         FROM   people_mentions p1,
                people_mentions p2,
                sentences
        WHERE   p1.sentence_id = p2.sentence_id
          AND   p1.sentence_id = sentences.sentence_id
          AND   p1.mention_id != p2.mention_id;
          """
      output_relation : "has_spouse"
      udf             : ${APP_HOME}"/udf/ext_has_spouse.py"

      # Run this extractor after "ext_people"
      dependencies    : ["ext_people"]
    }

  }
```

Note that this extractor must be executed after our previously added extractor
`ext_people`, so we specify the latter in the `dependencies` field.

When generating relation candidates, we also generate training data using
[distant supervision](../../general/distant_supervision.html). There are some
pairs of people that we know for sure are married, and we can use them as
training data for DeepDive. Similarly, if we know that two people are not
married, we can use them as negative training examples. In our case we will be
using data from [Freebase](http://www.freebase.com/) for distant supervision,
and use exact string matching to map mentions to entities.

To generate positive examples, we have exported all pairs of people with a
`has_spouse` relationship from the [Freebase data
dump](https://developers.google.com/freebase/data) and included them in a TSV
file `data/spouses.tsv`, which should have been downloaded in
[Preparation section](#preparation).

To generate negative examples, we use the following heuristics:

1. A pair of persons who are in some kind of relation that is incompatible with
a marriage relation can be treated as a negative example: if, for example, A is
B's parent / children / sibling, then A is not likely to be married to B. We
include a TSV file in `data/non-spouses.tsv` containing such relations sampled
from Freebase, which should have been downloaded in the archive.

2. A pair of the same person is a negative example of `has_spouse` relations,
e.g., "Barack Obama" cannot be married to "Barack Obama". 

3. If the existing knowledge base of married couples (the `data/spouses.tsv`
file) contains the fact that person A is married to person B and person C is
married to person D, then it is unlikely that person A is married to person C.

These supervision rules expose a typical property of distant supervision rules:
they may be not perfect, in the sense that we may mislabel some relation
candidates. This is not an issue: as long as a sufficient majority of the
supervised candidates are correctly supervised, the system will be able to
extract the information even if the signal given by the distant supervision is,
in some sense, "noisy". This is a desirable property, as it allows to use
distant supervision rules that are at least reasonable even if they are not
perfect.

We now create a script `udf/ext_has_spouse.py` to generate and label
the relation candidates:

```python
#! /usr/bin/env python

import csv, os, sys

# The directory of this UDF file
BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision.
# A person can have multiple spouses
spouses = set()
married_people = set()
lines = open(BASE_DIR + '/../data/spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  spouses.add((name1, name2))  # Add a spouse relation pair
  married_people.add(name1)    # Record the person as married
  married_people.add(name2)

# Load relations of people that are not spouse
# The non-spouse KB lists incompatible relations, e.g. childrens, siblings, parents.
non_spouses = set()
lines = open(BASE_DIR + '/../data/non-spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair

# For each input tuple
for row in sys.stdin:
  parts = row.strip().split('\t')
  sentence_id, p1_id, p1_text, p2_id, p2_text = parts

  p1_text = p1_text.strip()
  p2_text = p2_text.strip()
  p1_text_lower = p1_text.lower()
  p2_text_lower = p2_text.lower()

  # DS rule 1: true if they appear in spouse KB, 
  is_true = '\N'
  if (p1_text_lower, p2_text_lower) in spouses or \
     (p2_text_lower, p1_text_lower) in spouses:
    is_true = '1'
  # DS rule 2: false if they appear in non-spouse KB    
  elif (p1_text_lower, p2_text_lower) in non_spouses or \
       (p2_text_lower, p1_text_lower) in non_spouses:
    is_true = '0'
  # DS rule 3: false if they appear to be in same person
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = '0'
  # DS rule 4 false if they are both married, but not married to each other:
  elif p1_text_lower in married_people and p2_text_lower in married_people:
    is_true = '0'

  # Output relation candidates into output table
  print '\t'.join([
    p1_id, p2_id, sentence_id, 
    "%s-%s" %(p1_text, p2_text),
    is_true,
    "%s-%s" %(p1_id, p2_id),
    '\N'   # leave "id" blank for system!
    ])
```

We can now an run the system by executing `./run.sh` and check
the output relation `has_spouse`. `./run.sh` will run the full pipeline with all
extractors. If you only want to run the new
extractor, refer to the [Pipeline section in Extras](walkthrough-extras.html#pipelines).

We can look at some relation candidate generated by the `ext_has_spouse`
extractor:

```bash
./run.sh
[...]
psql -d deepdive_spouse -c "select * from has_spouse where person1_id='118238@10_7'"
```

The results will look like the following:

     person1_id  | person2_id  | sentence_id |         description         | is_true |       relation_id       |  id
    -------------+-------------+-------------+-----------------------------+---------+-------------------------+-------
     118238@10_7 | 118238@10_1 | 118238@10   | Michelle Obama-Barack Obama | t       | 118238@10_7-118238@10_1 | 

To check that your results are correct, you can count the number of tuples in
the table:

```bash
psql -d deepdive_spouse -c "select is_true, count(*) from has_spouse group by is_true;"
```

The results should be:

```
 is_true | count
---------+--------
         | 178426
 t       |  23570
 f       |  83344
```

#### <a name="candidate_relation_features" href="#"></a> Adding Features for candidate relations

In order for DeepDive to make predictions, we need to add *features* to our
candidate relations. Features are properties of the candidate that are used by
the system to decide whether or not the candidate is expressing a marriage
relation. For now, we use intuitive features, but this will lead to low quality
results which we will improve later. We now write an extractor that computes
features from the relation candidates and the sentences they come from.

The features we use are: 

1. the bag of words between the two mentions;

2. the number of words between two phases; 

3. whether the last word of the two persons' name (last name) is the same.

We will refine these features [later](walkthrough-improve.html).

For this new extractor:

**Input:** a mention pair as well as all words in the sentence it appears. e.g.:

    Sen.~^~Barack~^~Obama~^~and~^~his~^~wife~^~,~^~Michelle~^~Obama~^~,~^~have~^~released~^~eight~^~years~^~of~^~joint~^~returns~^~.	118238@10_7-118238@10_1	7	2	1	2

**Output:** all features for this mention pair described above:

    118238@10_1_118238@10_7	"word_between=,"
    118238@10_1_118238@10_7	"word_between=his"
    118238@10_1_118238@10_7	"potential_last_name_match"
    118238@10_1_118238@10_7	"word_between=wife"
    118238@10_1_118238@10_7	"num_words_between=4"
    118238@10_1_118238@10_7	"word_between=and"

Create a new extractor for features, which will execute after the
`ext_has_spouse_candidates` extractor:

```bash
  extraction.extractors {

    # ... (other extractors)

    # Extractor 4: extract features for relation candidates
    ext_has_spouse_features {
      style: "tsv_extractor"
      input: """
  		SELECT  array_to_string(words, '~^~'),
                has_spouse.relation_id,
                p1.start_position  AS  p1_start,
                p1.length          AS  p1_length,
                p2.start_position  AS  p2_start,
                p2.length          AS  p2_length
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
      dependencies    : ["ext_has_spouse_candidates"]
    }

  }
```

To create our extractor UDF, we make use of `ddlib`, our Python library that
provides useful utilities such as `Span` to manipulate elements in sentences.
Make sure you followed the [installation guide](../installation.html#ddlib) to
properly use `ddlib`.

Create the script `udf/ext_has_spouse_features.py` with the following content:

(a copy of this script is also available from 
`$DEEPDIVE_HOME/examples/tutorial_example/step1-basic/udf/ext_has_spouse_features.py`)


```python
#! /usr/bin/env python

import sys
import ddlib     # DeepDive python utility

ARR_DELIM = '~^~'

# For each input tuple
for row in sys.stdin:
  parts = row.strip().split('\t')
  if len(parts) != 6: 
    print >>sys.stderr, 'Failed to parse row:', row
    continue
  
  # Get all fields from a row
  words = parts[0].split(ARR_DELIM)
  relation_id = parts[1]
  p1_start, p1_length, p2_start, p2_length = [int(x) for x in parts[2:]]

  # Unpack input into tuples.
  span1 = ddlib.Span(begin_word_id=p1_start, length=p1_length)
  span2 = ddlib.Span(begin_word_id=p2_start, length=p2_length)

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

  for feature in features:  
    print str(relation_id) + '\t' + feature
```

As before, you can run the system by executing `run.sh` and check the output
relation `has_spouse_features`: 

```bash
./run.sh
[...]
psql -d deepdive_spouse -c "select * from has_spouse_features where relation_id = '118238@10_1_118238@10_7'"
```

The results would look like the following:

```
       relation_id       |          feature
-------------------------+---------------------------
 118238@10_1_118238@10_7 | word_between=,
 118238@10_1_118238@10_7 | word_between=his
 118238@10_1_118238@10_7 | potential_last_name_match
 118238@10_1_118238@10_7 | word_between=wife
 118238@10_1_118238@10_7 | num_words_between=4
 118238@10_1_118238@10_7 | word_between=and
(6 rows)
```

<!--
XXX Fix or remove

Again, you can count the number of tuples in the table:

```bash
psql -d deepdive_spouse -c "select count(*) from has_spouse_features;"
```

The results should be:

      count
    ---------
     1160450
-->

### <a name="inference_rules" href="#"></a> Step 3: Writing inference rules and defining holdout

Now we need to specify how DeepDive should generate the [factor
graph](../../general/inference.html) to perform probabilistic learning and inference.
We want to predict the `is_true` column of the `has_spouse` table based on the
features we have extracted, by assigning to each feature a weight that DeepDive
will learn from the training data. This is one of the simplest inference rules
one can write in DeepDive, as it does not involve any domain knowledge or
relationship among different random variables. 

Add the following lines to your `application.conf`, in the `inference.factors` block:

```bash
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
```

This rule generates a model similar to a logistic regression classifier: it uses
a set of features to make a prediction about the expectation of the variable we
are interested in. For each row in the *input query* we are creating a
[factor](../../general/inference.html) that is connected to the
`has_spouse.is_true` variable and whose weight is learned by DeepDive on the
basis of the `feature` column value (i.e., of the features).

Note that the syntax requires the users to **explicitly select** in the `input_query`:

1. The `id` column for each variable
2. The variable column, which is `is_true` in this case
3. The column the weight depends on (if any), which is `feature` in this case

When selecting these column, users must explicitly alias `id` to
`[relation_name].id` and `[variable]` to `[relation_name].[variable]` in order
for the system to use them. For additional information, refer to the [inference
rule guide](../inference_rules.html).

Now that we have defined inference rules, DeepDive will automatically
[ground](../overview.html#grounding) the factor graph using these rules, then
perform the learning to figure out the feature weights, and the inference to
compute the probabilities to associate to candidate relations.

We are almost ready to do our first complete run! In order to evaluate our
results, we also want to define a *holdout fraction* for our predictions. The
holdout fraction defines how much of our training data we want to treat as
testing data used to compare our predictions against. By default the holdout
fraction is `0`, which means that we cannot evaluate the precision of our
results. One may add a line `calibration.holdout_fraction: 0.25`
to `application.conf` to holdout one quarter of the training data randomly, 
but in our application, we instead specify a custom holdout SQL query which selects
the column `id` of some random rows from the `has_spouse` mention table and 
add them into the table `dd_graph_variables_holdout`.
Let's add it to `application.conf`:

```bash
calibration.holdout_query:"""
    DROP TABLE IF EXISTS holdout_sentence_ids CASCADE; 

    CREATE TABLE holdout_sentence_ids AS 
    SELECT sentence_id FROM sentences WHERE RANDOM() < 0.25;

    INSERT INTO dd_graph_variables_holdout(variable_id)
    SELECT id FROM has_spouse WHERE sentence_id IN
    (SELECT * FROM holdout_sentence_ids);
"""
```

At this point, the setup of the application is complete. Note that you can find
all extractors, scripts, and the complete `application.conf` file that we wrote
until now in the `$DEEPDIVE_HOME/examples/tutorial_example/step1-basic/`
directory.

### <a name="get_result" href="#"> </a> Step 4: Running and getting results

We can now run the application again

```bash
./run.sh
```

After running, you should see a summary report similar to:

    16:51:37 [profiler] INFO  --------------------------------------------------
    16:51:37 [profiler] INFO  Summary Report
    16:51:37 [profiler] INFO  --------------------------------------------------
    16:51:37 [profiler] INFO  ext_clear_table SUCCESS [330 ms]
    16:51:37 [profiler] INFO  ext_people SUCCESS [40792 ms]
    16:51:37 [profiler] INFO  ext_has_spouse_candidates SUCCESS [17194 ms]
    16:51:37 [profiler] INFO  ext_has_spouse_features SUCCESS [189242 ms]
    16:51:37 [profiler] INFO  inference_grounding SUCCESS [3881 ms]
    16:51:37 [profiler] INFO  inference SUCCESS [13366 ms]
    16:51:37 [profiler] INFO  calibration plot written to /YOUR/PATH/TO/deepdive/out/TIME/calibration/has_spouse.is_true.png [0 ms]
    16:51:37 [profiler] INFO  calibration SUCCESS [920 ms]
    16:51:37 [profiler] INFO  --------------------------------------------------


DeepDive creates a view `has_spouse_is_true_inference` that contains one row for
each variable for which it predicted the value. The schema of the view is the
same as the `has_spouse` table, with an additional `expectation` column
containing the predicted probability for the corresponding variable. We can run
the following query to sample some high-confidence mention-level relations and
the sentences they come from:

```bash
psql -d deepdive_spouse -c "
  SELECT s.sentence_id, description, is_true, expectation, s.sentence
  FROM has_spouse_is_true_inference hsi, sentences s
  WHERE s.sentence_id = hsi.sentence_id and expectation > 0.9
  ORDER BY random() LIMIT 5;
"
```

The result should look like the following (might not be the same):

```
 sentence_id |       description        | is_true | expectation | sentence
-------------+--------------------------+---------+-------------+----------
 154431@0    | Al Austin-Akshay Desai   |         |       0.972 | Guest list Jeff Atwater , Senate president Al Austin , Republican fundraiser from Tampa St. Petersburg Mayor Rick Ba
ker and wife , Joyce , St. Petersburg Brian Ballard , former chief of staff for Gov. Bob Martinez and a campaign adviser to Crist Rodney Barreto , a Crist fundraiser Ron Book , South
 Florida lobbyist Charlie Bronson , agriculture commissioner Dean Colson , a Miami lawyer and adviser to the governor on higher education issues Dr. Akshay Desai , head of Universal
Health Care Insurance in St. Petersburg Eric Eikenberg , Crist 's chief of staff Fazal Fazlin , entrepreneur -LRB- held fundraisers for Crist during the gubernatorial campaign -RRB-
, St. Petersburg .
 148797@53   | Michael-Carole Shelley   |         |       0.954 | WITH : Haydn Gwynne -LRB- Mrs. Wilkinson -RRB- , Gregory Jbara -LRB- Dad -RRB- , Carole Shelley -LRB- Grandma -RRB-
and Santino Fontana -LRB- Tony -RRB- , David Bologna and Frank Dolce -LRB- Michael -RRB- , and David Alvarez , Trent Kowalik and Kiril Kulish -LRB- Billy -RRB- .
 58364@10    | John-Jack Edwards        |         |       0.944 | This year , John and Jack Edwards are far from the only parent and child negotiating the awkward intersection of fam
ily and campaign life .
 40427@30    | James Taylor-Carly Simon |         |       0.962 | Look at James Taylor and Carly Simon , Whitney Houston and Bobby Brown or Britney Spears and Kevin Federline -LRB- y
es , calling him a musician is a stretch -RRB- .
 23278@18    | Chazz-Josh Gordon        |         |       0.944 | Directors Josh Gordon and Will Speck even discover an original way of hitting Chazz and Jimmy below the belt simulta
neously .
(5 rows)
```

We see that the results do not seem very good, but we will improve them in the
next section.  

Before that, let us mention the fact that  DeepDive generates [calibration
plots](../calibration.html) for all variables defined in the schema to help with
debugging. Let's take a look at the generated calibration plot, written to the
file outputted in the summary report above (has_spouse.is_true.png). It should
look something like this:

![Calibration]({{site.baseurl}}/assets/walkthrough_has_spouse_is_true.png)

The calibration plots contain useful information that help you to improve the
quality of your predictions. For actionable advice about interpreting
calibration plots, refer to the [calibration guide](../calibration.html). 

In the [next section](walkthrough-improve.html), we will discuss several ways to
analyze and improve the quality of our application.

