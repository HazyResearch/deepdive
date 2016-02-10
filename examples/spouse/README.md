# Tutorial
# Extracting Mentions of Spouses From The News

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
Accordingly, we'll walk through an example scenario where we wish to extract mentions of two people being spouses from news articles.

The high-level steps we'll follow are:

1. **_Inputs_: Loading Data, Candidate \& Feature Extraction** First, we'll extract a set of _candidate_ relation mentions, and a sparse _feature_ representation of each.

2. **_Labels_: Distant Supervision** Next, we'll use various strategies to provide _supervision_ for our dataset, so that we can use machine learning to learn the weights of a model.

3. **_Learning \& Inference_: Model Specification** Then, we'll specify the high-level configuration of our _model_.

4. **_Labeling, Error Analysis \& Debugging_** Finally, we'll show how to use DeepDive's labeling, error analysis and debugging tools.

*_Note the distinction between extraction of facts and mentions of facts. In this tutorial, we do the latter, however DeepDive supports further downstream methods for tackling the former task in a principled manner._

## _0. Basic setup_
First of all, make sure that DeepDive has been [installed](http://deepdive.stanford.edu/doc/basics/installation.html).

Next, DeepDive will store all data- input, intermediate, output, etc- in a relational database;
currently, Postgres, Greenplum, and MySQL are supported, however Greenplum or Postgres are strongly recommended.
To set the location of this database, we need to configure a URL in the `db.url` file, e.g.:
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

Next, we need to declare the schema of this `articles` table in our `app.ddlog` file; we add the following lines:
```
# example DeepDive application for finding spouse relationships in news articles
articles(
    id text,
    content text
).
```

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
Next, we'll use Stanford's [CoreNLP](http://stanfordnlp.github.io/CoreNLP/) natural language processing (NLP) system to add useful markups and structure to our input data.
This step will split up our articles into sentences, and their component _tokens_ (roughly, the words).
Additionally, we'll also get _lemmas_ (normalized word forms), _part-of-speech (POS) tags_, _named entity recognition (NER) tags_, and a dependency parse of the sentence.
We declare the output schema of this step in our `app.ddlog`:
```
sentences(
    doc_id         text,
    sentence_index int,
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

<!-- TODO let's drop this unless the key/distributed_by reveal something important
Note that we declare a compound key of `(doc_id, sentence_index)` for each sentence, and that we declare a `distributed_by` attribute, e.g., primarily for Greenplum, using DDlog annotations.
-->

Next we declare a DDlog function which takes in the `doc_id` and `content` for an article, and returns rows conforming to the sentences schema we just declared, using the **user-defined function (UDF)** in `udf/nlp_markup.sh`.
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
Note that the previous steps- here, loading the articles- will _not_ be re-run (unless we specify that they should be, using, e.g., `deepdive mark todo articles`).

### 1.3 Extracting Candidate Relation Mentions

#### Extracting People
Once again we first declare the schema:
```
person_mention(
    mention_id text,
    mention_text text,
    doc_id text,
    sentence_index int,
    begin_index int,
    end_index int
).
```
We will be storing each person as a row referencing a sentence and beginning and ending indexes.
Again we next declare a function which references a UDF, and takes as input the sentence tokens and NER tags:
```
function map_person_mention over (
        doc_id text,
        sentence_index int,
        tokens text[],
        ner_tags text[]
    ) returns rows like person_mention
    implementation "udf/map_person_mention.py" handles tsv lines.
```

We'll write a simple UDF in Python that will tag spans of contiguous tokens with the NER tag "PERSON" as person mentions (i.e., we'll essentially rely on CoreNLP's NER module).
Note that we've already used a bash script as a UDF, and indeed any programming language can be used (DeepDive will just check the path specified in the top line, e.g., `#!/usr/bin/env python`)/
However DeepDive provides some convenient utilities for Python UDFs which handle all IO encoding/decoding.
To write our UDF, we'll start by specifying that our UDF will handle tsv lines (as specified in the DDlog above);
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
    """
    Finds phrases that are continuous words tagged with PERSON.
    """
    num_tokens = len(ner_tags)
    # find all first indexes of series of tokens tagged as PERSON
    first_indexes = (i for i in xrange(num_tokens) if ner_tags[i] == "PERSON" and (i == 0 or ner_tags[i-1] != "PERSON"))
    for begin_index in first_indexes:
        # find the end of the PERSON phrase (consecutive tokens tagged as PERSON)
        end_index = begin_index + 1
        while end_index < num_tokens and ner_tags[end_index] == "PERSON":
            end_index += 1
        end_index -= 1
        # generate a mention identifier
        mention_id = "%s_%d_%d_%d" % (doc_id, sentence_index, begin_index, end_index)
        mention_text = " ".join(map(lambda i: tokens[i], xrange(begin_index, end_index + 1)))
        # Output a tuple for each PERSON phrase
        yield [
            mention_id,
            mention_text,
            doc_id,
            sentence_index,
            begin_index,
            end_index,
        ]
```
Above, we write a simple function which extracts and tags all subsequences of tokens having the NER tag "PERSON".
Note that the `extract` function must be a generator, i.e., use a `yield` statement to return output rows.

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
Again, to start, we declare the schema for our `spouse_candidate` table- here just the two names, and the two person_mention IDs referred to:
```
spouse_candidate(
    p1_id text,
    p1_name text,
    p2_id text,
    p2_name text
).
```

Next, for this operation we don't use any UDF script, instead relying entirely on DDlog operations.
We simply construct a table of person counts, and then do a join with our filtering conditions; in DDlog this looks like:
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
spouse_feature(
    p1_id text,
    p2_id text,
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
However, we **don't have any supervised labels** (i.e., a set of correct answers) for a machine learning algorithm to learn from!
In most real world applications, a sufficiently large set of supervised labels is _not_ in fact available.
With DeepDive, we take the approach sometimes refered to as _distant supervision_ or _data programming_, where we instead generate a **noisy set of labels using a mix of mappings from secondary datasets \& other heuristic rules**.


## 2. _Labels_: Distant Supervision
In this section, we'll use _distant supervision_ (or '_data programming_') to provide a noisy set of labels to supervise our candidate relation mentions, based on which we can train a machine learning model.

We'll describe two basic categories of approaches:

1.  Mapping from secondary data for distant supervision
2.  Using heuristic rules for distant supervision

Finally, we'll describe a simple majority-vote approach to resolving multiple labels per example, which can be implemented within DDlog.

### 2.1 Mapping from Secondary Data for Distant Supervision
First, we'll try using an external structured dataset of known married couples, from [DBpedia](http://wiki.dbpedia.org/), to distantly supervise our dataset.
We'll download the relevant data, and then map it to our spouse candidate mentions.

#### Extracting \& Downloading the DBpedia Data
Our goal is to first extract a collection of known married couples from DBpedia and then load this into the `spouses_dbpedia` table in our database.
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
Notice that for DeepDive to load the data to the corresponding database table the name of the input data again has to be stored in the directory `input/` and has the same name as the target database table. To load the data we execute the command:
```bash
deepdive do spouses_dbpedia
```

#### Supervising Spouse Candidates with DBpedia Data
First we'll declare a new table where we'll store the labels (referring to the spouse candidate mentions), with an integer value (`True=1, False=-1`) and a description (`rule_id`):
```
spouse_label(
    p1_id text,
    p2_id text,
    label int,
    rule_id text
).
```
Next we'll implement a simple distant supervision rule which labels any spouse mention candidate with a pair of names appearing in DBpedia as true:
```
# distant supervision using data from DBpedia
spouse_label(p1,p2, 1, "from_dbpedia") :-
  spouse_candidate(p1, p1_name, p2, p2_name), spouses_dbpedia(n1, n2),
  [ lower(n1) = lower(p1_name), lower(n2) = lower(p2_name) ;
    lower(n2) = lower(p1_name), lower(n1) = lower(p2_name) ].
```
It should be noted that there are many clear ways in which this rule could be improved (fuzzy matching, more restrictive conditions, etc.), but this serves as an example of one major type of distant supervision rule.

### 2.2 Using heuristic rules for distant supervision
We can also create a supervision rule which does not rely on any secondary structured dataset like DBpedia, but instead just uses some heuristic.
We set up a DDlog function, `supervise`, which uses a UDF containing several heuristic rules over the mention and sentence attributes:
```
function supervise over (
        p1_id text, p1_begin int, p1_end int,
        p2_id text, p2_begin int, p2_end int,
        doc_id         text,
        sentence_index int,
        sentence_text  text,
        tokens         text[],
        lemmas         text[],
        pos_tags       text[],
        ner_tags       text[],
        dep_types      text[],
        dep_tokens    int[]
    ) returns (
        p1_id text, p2_id text, label int, rule_id text
    )
    implementation "udf/supervise_spouse.py" handles tsv lines.

spouse_label += supervise(
    p1_id, p1_begin, p1_end,
    p2_id, p2_begin, p2_end,
    doc_id, sentence_index, sentence_text,
    tokens, lemmas, pos_tags, ner_tags, dep_types, dep_token_indexes
) :- spouse_candidate(p1_id, _, p2_id, _),
    person_mention(p1_id, p1_text, doc_id, sentence_index, p1_begin, p1_end),
    person_mention(p2_id, p2_text,      _,              _, p2_begin, p2_end),
    sentences(
        doc_id, sentence_index, sentence_text,
        tokens, lemmas, pos_tags, ner_tags, _, dep_types, dep_token_indexes
    ).
```

The Python UDF contains several heuristic rules:
* Candidates with person mentions that are too far apart in the sentence are marked as false.
* Candidates with person mentions that have another person in between are marked as false.
* Candidates with person mentions that have words like "wife" or "husband" in between are marked as true.
* Candidates with person mentions that have "and" in between and "married" after are marked as true.
* Candidates with person mentions that have familial relation words in between are marked as false.
```python
from deepdive import *
import random
from collections import namedtuple

SpouseLabel = namedtuple('SpouseLabel', 'p1_id, p2_id, label, type')

@tsv_extractor
@returns(lambda
        p1_id =   "text",
        p2_id =   "text",
        label =   "int" ,
        rule_id = "text",
    :[])
# heuristic rules for finding positive/negative examples of spouse relationship mentions
def supervise(
      p1_id="text", p1_begin="int", p1_end="int",
      p2_id="text", p2_begin="int", p2_end="int",
      doc_id="text", sentence_index="int", sentence_text="text",
      tokens="text[]", lemmas="text[]", pos_tags="text[]", ner_tags="text[]",
      dep_types="text[]", dep_token_indexes="int[]",
  ):

  # Constants
  MARRIED = frozenset(["wife", "husband"])
  FAMILY = frozenset(["mother", "father", "sister", "brother", "brother-in-law"])
  MAX_DIST = 10

  # Common data objects
  p1_end_idx = min(p1_end, p2_end)
  p2_start_idx = max(p1_begin, p2_begin)
  p2_end_idx = max(p1_end,p2_end)
  intermediate_lemmas = lemmas[p1_end_idx+1:p2_start_idx]
  intermediate_ner_tags = ner_tags[p1_end_idx+1:p2_start_idx]
  tail_lemmas = lemmas[p2_end_idx+1:]
  spouse = SpouseLabel(p1_id=p1_id, p2_id=p2_id, label=None, type=None)

  # Rule: Candidates that are too far apart
  if len(intermediate_lemmas) > MAX_DIST:
    yield spouse._replace(label=-1, type='neg:far_apart')

  # Rule: Candidates that have a third person in between
  if 'PERSON' in intermediate_ner_tags:
    yield spouse._replace(label=-1, type='neg:third_person_between')

  # Rule: Sentences that contain wife/husband in between
  #         (<P1>)([ A-Za-z]+)(wife|husband)([ A-Za-z]+)(<P2>)
  if len(MARRIED.intersection(intermediate_lemmas)) > 0:
      yield spouse._replace(label=1, type='pos:wife_husband_between')

  # Rule: Sentences that contain and ... married
  #         (<P1>)(and)?(<P2>)([ A-Za-z]+)(married)
  if ("and" in intermediate_lemmas) and ("married" in tail_lemmas):
      yield spouse._replace(label=1, type='pos:married_after')

  # Rule: Sentences that contain familial relations:
  #         (<P1>)([ A-Za-z]+)(brother|stster|father|mother)([ A-Za-z]+)(<P2>)
  if len(FAMILY.intersection(intermediate_lemmas)) > 0:
      yield spouse._replace(label=-1, type='neg:familial_between')
```

Note that the rough theory behind this approach is that we don't need high-quality, e.g., hand-labeled supervision to learn a high quality model;
instead, using statistical learning, we can in fact recover high-quality models from a large set of low-quality- or **_noisy_**- labels.

### 2.3 Resolving Multiple Labels Per Example with Majority Vote
Finally, we implement a very simple majority vote procedure, all in DDlog, for resolving scenarios where a single spouse candidate mention has multiple conflicting labels.
First, we sum the labels (which are all -1, 0, or 1):
```
spouse_label_resolved(p1_id, p2_id, SUM(vote)) :- spouse_label(p1_id, p2_id, vote, rule_id).
```
Then, we simply threshold, and add these labels to our decision variable table `has_spouse` (see next section for details here):
```
has_spouse(p1_id, p2_id) = if l > 0 then TRUE
                      else if l < 0 then FALSE
                      else NULL end :- spouse_label_resolved(p1_id, p2_id, l).
```
We additionally make sure that all spouse candidate mentions _not_ labeled by a rule are also included in this table:
```
has_spouse(p1, p2) = NULL :- spouse_candidate(p1, _, p2, _).
```

Once again, to execute all of the above, just run `deepdive compile && deepdive do has_spouse` (recall that `deepdive do` will execute all upstream tasks as well, so this will execute all of the previous steps!).


## 3. _Learning \& Inference_: Model Specification
Now, we need to specify the actual model that DeepDive will perform learning and inference over.
At a high level, this boils down to specifying three things:

1.  What are the _variables_ of interest, that we want DeepDive to predict for us?

2.  What are the _features_ for each of these variables?

3.  What are the _connections_ between the variables?

One we have specified the model in this way, DeepDive will _learn_ the parameters of the model (the weights of the features and potentially of the connections between variables), and then perform _statistical inference_ over the learned model to determine the most likely values of the variables of interest.

For more advanced users: we are specifying a _factor graph_ where the features are unary factors, and then using SGD and Gibbs Sampling for learning and inference; further technical detail is available [here](#).

### 3.1 Specifying Prediction Variables
In our case, we have one variable to predict per spouse candidate mention, namely, **is this mention actually indicating a spousal relation or not?**
In other words, we want DeepDive to predict the value of a boolean variable for each spouse candidate mention, indicating whether it is true or not.
We specify this in `app.ddlog` as follows:
```
has_spouse?(
    p1_id text,
    p2_id text
).
```

DeepDive will predict not only the value of these variables, but also the marginal probabilities, i.e., the amount of confidence DeepDive has for each individual prediction.

### 3.2 Specifying Features
Next, we indicate (i) that each `has_spouse` variable will be connected to the features of the corresponding `spouse_candidate` row, (ii) that we wish DeepDive to learn the weights of these features from our distantly supervised data, and (iii) that the weight of a specific feature across all instances should be the same, as follows:
```
@weight(f)
has_spouse(p1_id, p2_id) :-
  spouse_candidate(p1_id, _, p2_id, _),
  spouse_feature(p1_id, p2_id, f).
```

### 3.3 Specifying Connections Between Variables
Finally, we can specify relations between the prediction variables, with either learned or given weights.
Here, we'll specify two such rules, with fixed (given) weights that we specify.
First, we define a _symmetry_ connection, namely specifying that if the model thinks a person mention `p1` and a person mention `p2` indicate a spousal relationship in a sentence, then it should also think that the reverse is true, i.e., that `p2` and `p1` indicate one too:
```
@weight(3.0)
has_spouse(p1_id, p2_id) => has_spouse(p2_id, p1_id) :-
  spouse_candidate(p1_id, _, p2_id, _).
```

Next, we specify a rule that the model should be strongly biased towards finding one marriage indication per person mention.
We do this inversely, using a negative weight, as follows:
```
@weight(-1.0)
has_spouse(p1_id, p2_id) => has_spouse(p1_id, p3_id) :-
  spouse_candidate(p1_id, _, p2_id, _),
  spouse_candidate(p1_id, _, p3_id, _).
```

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
