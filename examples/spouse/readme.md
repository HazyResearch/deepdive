# Tutorial: Extracting Mentions of Spouses From The News

In this tutorial, we show an example of a prototypical task that DeepDive is often applied to:
extraction of _structured information_ from _unstructured or 'dark' data_ such as webpages, text documents, images, etc.
While DeepDive can be used as a more general platform for statistical learning and data processing,
most of the tooling described herein has been built for this type of use case,
based on our experience successfully applying DeepDive to [a variety of real-world problems of this type](http://deepdive.stanford.edu/doc/showcase/apps.html).

In this setting, our goal is to take in a set of unstructured (and/or structured) inputs,
and populate a relation database table with extracted outputs,
along with marginal probabilities for each extraction representing DeepDive's confidence in the extraction.
More formally, we write a DeepDive application to extract mentions of _relations_ and their constituent _entities_ or _attributes_, according to a specified schema;
this task is often referred to as **_relation extraction_**.*
Accordingly, we'll walk through an example scenario where we wish to extract mentions of
spousal relations from news articles.

The high-level steps we'll follow are:

1. **_Inputs_: Loading Data, Candidate \& Feature Extraction** First, we'll extract a set of _candidate_ relation mentions, and a sparse _feature_ representation of each.

2. **_Labels_: Distant Supervision** Next, we'll use various strategies to provide _supervision_ for our dataset, so that we can use machine learning to learn the weights of a model.

3. **_Inference \& Learning_: Model Specification** Then, we'll specify the high-level configuration of our _model_.

4. **_Labeling, Error Analysis \& Debugging_** Finally, we'll show how to use DeepDive's labeling, error analysis and debugging tools.

*_Note the distinction between extraction of facts and mentions of facts. In this tutorial, we do the latter, however DeepDive supports further downstream methods for tackling the former task in a principled manner._

## _0. Basic setup_
First of all, make sure that DeepDive has been [installed](http://deepdive.stanford.edu/doc/basics/installation.html).

Next, DeepDive will store all data- input, intermediate, output, etc- in a relational database;
currently, Postgres, Greenplum, and MySQL are supported, however Greenplum or Postgres are strongly recommended.
To set the location of this database, we need to define a `db.url` file, e.g.:
```bash
echo "[postgresql|greenplum]://[USER]@[HOST]:[PORT]/deepdive_spouse" > db.url
```
_Note: DeepDive will drop and then create this database if run from scratch- beware of pointing to an existing populated one!_


## 1. _Inputs_: Loading Data, Candidate \& Feature Extraction

In this section, we'll generate the traditional inputs of a statistical learning-type problem: candidate objects, represented by a set of features, which we will aim to classify as _actual_ spouse relation mentions or not.

We'll do this in four basic steps:

1.  _Loading raw input data_
2.  _Preprocessing input data_
3.  _Extracting candidate relation mentions_
4.  _Extracting features for each candidate_

### 1.1 Loading Raw Input Data
Our goal, first of all, is to download and load the raw text of the [articles](http://research.signalmedia.co/newsir16/signal-dataset.html)
into an `articles` table in our database.
We create a simple shell script that downloads & outputs the news articles in `tsv` format.
DeepDive will automatically create the table, execute the script and load the table if we save it as:
```bash
input/articles.tsv.sh
```
The aforementioned script reads a sample of the corpus (provided as lines of json objects), and then using the [jq](https://stedolan.github.io/jq/) language extracts the fields `id` (for docuemnt id) and `content` from each entry and converts those to `tsv` format.

Next, we need to define the schema of this `articles` table in our `app.ddlog` file; we add the following lines:
```
# example DeepDive application for finding spouse relationships in news articles
articles(
    id text,
    content text
).
```
Note that we add decorators to denote that this is an input _source_,
that the article id is a _key_ for the relation, and that the raw content should be _searchable_ in our analysis tools; more on this later on!

Next, we compile our application, as we must do whenever we change `app.ddlog`:
```bash
deepdive compile
```

Finally, we tell DeepDive to execute the steps to load the `articles` table:
```bash
deepdive do articles
```
DeepDive will output an execution plan, which will pop up in your default text editor;
save and exit to accept, and DeepDive will run, creating the table and then fetching & loading the data!

### 1.2 Preprocessing input data
Next, we'll use Stanford's [CoreNLP](http://stanfordnlp.github.io/CoreNLP/) natural language processing (NLP) system to add useful annotations and structure to our input data.
This step will split up our articles into sentences, and their component _tokens_ (roughly, the words).
Additionally, we'll also get _lemmas_ (normalized word forms), _part-of-speech (POS) tags_, _named entity recognition (NER) tags_, and a dependency parse of the sentence.
We specify the output schema of this step in our `app.ddlog`:
```
@source
sentences(
    @key
    @distributed_by
    @references(relation="articles", column="id")
    doc_id         text,
    @key
    sentence_index int,
    @searchable
    sentence_text  text,
    tokens         text[],
    lemmas         text[],
    pos_tags       text[],
    ner_tags       text[],
    doc_offsets    int[],
    dep_types      text[],
    dep_tokens     int[]
).
```
Note that we define a compound key of `(doc_id, sentence_index)` for each sentence, and that we define a `distributed_by` attribute for e.g. Greenplum, using ddlog decorators.

Next we define a ddlog function which takes in the `doc_id` and `content` for an article, and returns rows conforming to the sentences schema we just defined, using the **user-defined function (UDF)** in `udf/nlp_markup.sh`.
This UDF is a bash script which calls a [wrapper](https://github.com/HazyResearch/bazaar/tree/master/parser) around CoreNLP.
```
function nlp_markup over (
        doc_id text,
        content text
    ) returns rows like sentences
    implementation "udf/nlp_markup.sh" handles tsv lines.
```

Finally, we specify that this `nlp_markup` function should be run over each row from `articles`, and the output appended to `sentences`: 
```
sentences +=
  nlp_markup(doc_id, content) :-
  articles(doc_id, content).
```

Again, to execute, we compile and then run:
```bash
deepdive compile
deepdive do sentences
```
Note that the previous steps- here, loading the articles- will _not_ be re-run (unless we specify that they should be, using e.g. `deepdive mark todo articles`).

### 1.3 Extracting Candidate Relation Mentions

#### Extracting People
Once again we first define the schema:
```
@extraction
person_mention(
    @key
    mention_id text,
    @searchable
    mention_text text,
    @distributed_by
    @references(relation="sentences", column="doc_id",         alias="appears_in")
    doc_id text,
    @references(relation="sentences", column="sentence_index", alias="appears_in")
    sentence_index int,
    begin_index int,
    end_index int
).
```
We will be storing each person as a row referencing a sentence and beginning and ending indexes.
Again we next define a function which references a UDF, and takes as input the sentence tokens and NER tags:
```
function map_person_mention over (
        doc_id text,
        sentence_index int,
        tokens text[],
        ner_tags text[]
    ) returns rows like person_mention
    implementation "udf/map_person_mention.py" handles tsv lines.
```

We'll write a simple UDF in Python that will tag spans of contiguous tokens with the NER tag "PERSON" as person mentions (i.e. we'll essentially rely on CoreNLP's NER module).
Note that we've already used a bash script as a UDF, and indeed any programming language can be used (DeepDive will just check the path specified in the top line, e.g. `#!/usr/bin/env python`)/ 
However DeepDive provides some convenient utilities for Python UDFs which handle all IO encoding/decoding.
To write our UDF, we'll start by specifying that our UDF will handle tsv lines (as specified in the ddlog above);
additionally we'll specify the exact type schema of both input and output, which DeepDive will check for us:
```python
from deepdive import *

@tsv_extractor
@returns(lambda
        mention_id       = "text" ,
        mention_text     = "text" ,
        doc_id           = "text" ,
        sentence_index   = "int"  ,
        begin_index      = "int"  ,
        end_index        = "int"  ,
    :[])
def extract(doc_id="text", sentence_index="int", tokens="text[]", ner_tags="text[]"):
```
Nest, we go on to write a simple function which extracts and tags all subsequences of tokens having the NER tag "PERSON".
Note that the `extract` function must be a generator, i.e. use a `yield` statement to return output rows.
To see the full implemention, refer to the source code in `udf/map_person_mention.py`.

Finally, we specify that the function will be applied to rows from the `sentences` table and append to the `person_mention` table:
```
person_mention += map_person_mention(
    doc_id, sentence_index, tokens, ner_tags
) :- sentences(doc_id, sentence_index, _, tokens, _, _, ner_tags, _, _, _).
```
Again, to run, just compile \& execute- `deepdive compile && deepdive do person_mention`- as in previous steps.

#### Extracting Candidate Spouses (Pairs of People)
Next, we'll take all pairs of **non-overlapping person mentions that co-occur in a sentence with less than 5 people total,** and consider these as the set of potential ('candidate') spouse mentions.
We thus filter out sentences with large numbers of people for the purposes of this tutorial; however these could be included if desired.
Again, to start, we define the schema for our `spouse_candidate` table- here just the two names, and the two person_mention IDs referred to:
```
spouse_candidate(
    p1_id text,
    p1_name text,
    p2_id text,
    p2_name text
).
```

Next, for this operation we don't use any UDF script, instead relying entirely on DDLOG operations.
We simply construct a table of person counts, and then do a join with our filtering conditions; in DDLOG this looks like:
```
num_people(doc_id, sentence_index, COUNT(p)) :-
    person_mention(p, _, doc_id, sentence_index, _, _).

spouse_candidate(p1, p1_name, p2, p2_name) :-
    num_people(same_doc, same_sentence, num_p),
    person_mention(p1, p1_name, same_doc, same_sentence, p1_begin, _),
    person_mention(p2, p2_name, same_doc, same_sentence, p2_begin, _),
    num_p < 5,
    p1_name != p2_name,
    p1_begin != p2_begin.
```
Again, to run, just compile \& execute- `deepdive compile && deepdive do spouse_candidate`- as in previous steps.

### 1.4 Extracting Features for each Candidate
Finally, we will extract a set of **features** for each candidate:
```
@extraction
spouse_feature(
    @key
    @references(relation="has_spouse", column="p1_id", alias="has_spouse")
    p1_id text,
    @key
    @references(relation="has_spouse", column="p2_id", alias="has_spouse")
    p2_id text,
    @key
    feature text
).
```
The goal here is to represent each spouse candidate mention by a set of attributes or **_features_** which capture at least the key aspects of the mention, and then let a machine learning model learn how much each feature is correlated with our decision variable ('is this a spouse mention?').
For those who have worked with machine learning systems before, note that we are using a sparse storage represenation-
you could think of a spouse candidate `(p1_id, p2_id)` as being represented by a vector of length `L = count(distinct(feature))`, consisting of all zeros except for at the indexes specified by the rows with key `(p1_id, p2_id)`.

DeepDive includes an automatic feature generation library, DDLIB, which we will use here.
Although many state-of-the-art [applications](http://deepdive.stanford.edu/doc/showcase/apps.html) have been built using purely DDLIB-generated features, others can be used and/or added as well.  To use DDLIB, we create a list of ddlib `Word` objects, two ddlib `Span` objects, and then use the function `get_generic_features_relation`:
```python
from deepdive import *
import ddlib

@tsv_extractor
@returns(lambda
        p1_id   = "text" ,
        p2_id   = "text" ,
        feature = "text" ,
    :[])
def extract(p1_id="text", p2_id="text", p1_begin_index="int", p1_end_index="int", p2_begin_index="int", p2_end_index="int", doc_id="text", sent_index="int", tokens="text[]", lemmas="text[]", pos_tags="text[]", ner_tags="text[]", dep_types="text[]", dep_parents="int[]"):
    """
    Uses DDLIB to generate features for the spouse relation.
    """
    # Create a DDLIB sentence object, which is just a list of DDLIB Word objects
    sent = []
    for i,t in enumerate(tokens):
      sent.append(ddlib.Word(
        begin_char_offset=None,
        end_char_offset=None,
        word=t,
        lemma=lemmas[i],
        pos=pos_tags[i],
        ner=ner_tags[i],
        dep_par=dep_parents[i] - 1,  # Note that as stored from CoreNLP 0 is ROOT, but for DDLIB -1 is ROOT
        dep_label=dep_types[i]))

    # Create DDLIB Spans for the two person mentions
    p1_span = ddlib.Span(begin_word_id=p1_begin_index, length=(p1_end_index-p1_begin_index+1))
    p2_span = ddlib.Span(begin_word_id=p2_begin_index, length=(p2_end_index-p2_begin_index+1))

    # Generate the generic features using DDLIB
    for feature in ddlib.get_generic_features_relation(sent, p1_span, p2_span):
      yield [p1_id, p2_id, feature]
```
Note that getting the input for this UDF requires joining the `person_mention` and `sentences` tables:
```
function extract_spouse_features over (
        p1_id text,
        p2_id text,
        p1_begin_index int,
        p1_end_index int,
        p2_begin_index int,
        p2_end_index int,
        doc_id text,
        sent_index int,
        tokens text[],
        lemmas text[],
        pos_tags text[],
        ner_tags text[],
        dep_types text[],
        dep_tokens int[]
    ) returns rows like spouse_feature
    implementation "udf/extract_spouse_features.py" handles tsv lines.

spouse_feature += extract_spouse_features(
  p1_id, p2_id, p1_begin_index, p1_end_index, p2_begin_index, p2_end_index,
  doc_id, sent_index, tokens, lemmas, pos_tags, ner_tags, dep_types, dep_tokens) :-
  person_mention(p1_id, _, doc_id, sent_index, p1_begin_index, p1_end_index),
  person_mention(p2_id, _, doc_id, sent_index, p2_begin_index, p2_end_index),
  sentences(doc_id, sent_index, _, tokens, lemmas, pos_tags, ner_tags, _, dep_types, dep_tokens
).
```
Again, to run, just compile \& execute- `deepdive compile && deepdive do spouse_feature`- as in previous steps.

Now we have generated what looks more like the standard input to a machine learning problem- a set of objects, represented by sets of features, which we want to classify (here, as true or false mentions of a spousal relation).
However, we **don't have any supervised labels** (i.e. a set of correct answers) for a machine learning algorithm to learn from!
In most real world applications, a sufficiently large set of supervised labels is _not_ in fact available.
With DeepDive, we take the approach sometimes refered to as _distant supervision_ or _data programming_, where we instead generate a **noisy set of labels using a mix of mappings from secondary datasets \& other heuristic rules**.


## 2. _Labels_: Distant Supervision
In this section, we'll use _distant supervision_ (or '_data programming_') to provide a noisy set of labels to supervise our candidate relation mentions, based on which we can train a machine learning model.

We'll describe this in three basic steps:

1.  Loading secondary data for distant supervision
2.  Mapping to our dataset
3.  Other heuristic rules for distant supervision

### 2.1 Generating and Loading the Distant Supervision Data
Our goal is to first extract a collection of known married couples from DBpedia and then load this into the `spouses_dbpedia` table in our database.

#### Extracting the DBpedia Data
To extract known married couples, we used the DBpedia dump present in [Google's BigQuery platform](https://bigquery.cloud.google.com).
First we extracted the URI, name and spouse information from the dbpedia `person` table records in BigQuery for which the field `name` is not NULL. We used the following query:

```
SELECT URI,name, spouse
FROM [fh-bigquery:dbpedia.person]
where name <> "NULL"
```
We stored the result of the above query in a local project table `dbpedia.validnames` and perform a self-join to obtain the pairs of married couples.

```
SELECT t1.name, t2.name
FROM [dbpedia.validnames] AS t1
JOIN EACH [dbpedia.validnames] AS t2
ON t1.spouse = t2.URI
```
The output of the above query was stored in a new table named `dbpedia.spouseraw`. Finally, we used the following query to remove symmetric duplicates.

```
SELECT p1, p2
FROM
  (SELECT t1_name as p1, t2_name as p2
  FROM [dbpedia.spouseraw]),
  (SELECT t2_name as p1, t1_name as p2
  FROM [dbpedia.spouseraw])
WHERE p1 < p2
```
The output of this query was stored in a local file `spousesraw.csv`. The file contained duplicate rows (BigQuery does not support `distinct`) and noisy rows where the name field contained a string where the given name family name and multiple aliases where concatenated and reported in a string including the characters `{` and `}`. Using the unix commands `sed`, `sort` and `uniq` we first removed the lines containing characters `{` and `}` and then duplicate entries. This resulted in an input file `spouses_dbpedia.csv` containing 6,126 entries of married couples.

#### Loading to Database

We compress and store `spouses_dbpedia.csv` under the path:
```bash
input/spouses_dbpedia.csv.bz2
```
Notice that for DeepDive to load the data to the corresponding database table the name of the input data has to be stored in the directory `input/` and has the same name as the target database table. To load the data we execute the command:
```bash
deepdive do spouses_dbpedia
```
### 2.2 Mapping to our dataset
_**TODO**_

### 2.3 Other heuristic rules for distant supervision
_**TODO**_

## 3. _Inference \& Learning_: Model Specification
_**TODO**_

## 4. _Labeling, Error Analysis \& Debugging_
_**TODO**_

### Corpus Exploration with Mindbender (Optional)

This part of the tutorial is optional and focuses on how the user can browse through the input corpus via an automatically generated web-interface. The reader can safelly skip this part.

#### DDlog Annotations for Automated Mindtagger
```
@source
articles(
    @key
    id text,
    @searchable
    content text
).
```

#### Installing Mindbender
**_TODO: Put in proper way to do this!?_**
Given that `DEEPDIVE_ROOT` is a variable containing the path to the root of the deepdive repo, if you are on linux run:
```bash
wget -O ${DEEPDIVE_ROOT}/dist/stage/bin/mindbender https://github.com/HazyResearch/mindbender/releases/download/v0.2.1/mindbender-v0.2.1-Linux-x86_64.sh
```
for other versions see [the releases page](https://github.com/HazyResearch/mindbender/releases).  Then make sure that this location is on your path:
```bash
export PATH=${DEEPDIVE_ROOT}/dist/stage/bin:$PATH
```

#### Running Mindbender
First, generate the input for mindtagger.  You can edit the template for the data generated by editing `generate-input.sql`, and the template for displaying the data in `mindtagger.conf` and `template.html` (for more detail, see the [documentation](http://deepdive.stanford.edu/doc/basics/labeling.html))then run:
```bash
cd mindtagger
psql -d deepdive_spouse -f generate-input.sql > input.csv
```
Next, start mindtagger:
```bash
PORT=$PORT ./start-mindtagger.sh
```
Then navigate to the URL displayed in your browser.

2. Describe how to setup mindbender.
3. Describe which commands to run to get the mindbender environment up and running.
