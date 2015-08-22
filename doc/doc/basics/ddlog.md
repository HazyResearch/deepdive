---
layout: default
title: Writing Applications in DDlog
---

# Writing Applications in DDlog

DDlog is a higher-level language for writing DeepDive applications in succinct Datalog-like syntax.
We are gradually extending the language to allow expression of advanced SQL queries used by complex extractors as well as a variety of factors with Markov Logic-like syntax.

## Writing a DDlog Program

A DDlog program consists of the following parts:

1. Schema declarations for your source data, intermediate data, and variables.
2. Data transformation rules for extracting candidates, supervision labels, and features.
3. Scope rules for defining the domain of the variables.
4. Inference rules for describing dependency between the variables.

We show how each part should look like using the [spouse example in our walk through](walkthrough/walkthrough.html).
All DDlog code should be placed in a file named `app.ddlog` under the DeepDive application.
A complete example written in DDlog can be found from [examples/spouse_example/postgres/ddlog](https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog)


### Basic Syntax

Each declaration and rule ends with a period (`.`).
The order of the rules have no meaning.
Comments in DDlog begin with a hash (`#`) character.

### Schema Declaration

First of all, we declare the relations we use throughout the program.
The order doesn't matter, but it's a good idea to place them at the beginning because that makes it easier to understand.

#### Source Relations
We need a relation for holding raw text of all source documents.
Each relation has multiple columns whose name and type are declared one at a time separated by commas.

```
articles(
  article_id text,
  text       text).
```

#### Intermediate Relations
We'll populate the following `sentences` relation by parsing sentences from every articles and collecting NLP tags.

```
sentences(
  document_id     text,
  sentence        text,
  words           text[],
  lemma           text[],
  pos_tags        text[],
  dependencies    text[],
  ner_tags        text[],
  sentence_offset int,
  sentence_id     text).
```

Then, we'll map candidate mentions of people in each sentence using the following `people_mentions` relation.

```
people_mentions(
  sentence_id    text,
  start_position int,
  length         int,
  text           text,
  mention_id     text).
```

Candidate mentions of spouse relationships from them will then be stored in `has_spouse_candidates`.

```
has_spouse_candidates(
  person1_id  text,
  person2_id  text,
  sentence_id text,
  description text,
  relation_id text,
  is_true boolean).
```

For each relationship candidate, we'll extract features.

```
has_spouse_features(
  relation_id text,
  feature     text).
```

#### Variable Relations
Finally, we declare a variable relation that we want DeepDive to predict the marginal probability for us.

```
has_spouse?(relation_id text).
```

### Candidate Mapping and Supervision Rules
A user-defined function for mapping candidates and supervision them is written in Python and declared as below with a name `ext_people`.
The function is supposed to take as input a triple of sentence id, array of words, and array of NER tags of the words and output rows like the `people_mentions` relation.
The Python implementation `udf/ext_people.py` takes the input as tab-separated values in each line and  outputs in the same format.

```
function ext_people over like ext_people_input
                 returns like people_mentions
  implementation "udf/ext_people.py" handles tsv lines.
```

Then this user-defined function `ext_people` can be called in the following way to add more tuples to `people_mentions` taking triples from the `sentences` relation:

```
people_mentions :- !ext_people(ext_people_input).
ext_people_input(s, ARRAY_TO_STRING(words, "~^~"), ARRAY_TO_STRING(ner_tags, "~^~")) :-
  sentences(_1, _2, words, _3, _4, _5, ner_tags, _6, s).
```

In a similar way, we can have another UDF map candidate relationships and supervise them.

```
function ext_has_spouse over like ext_has_spouse_input
                     returns like has_spouse_candidates
  implementation "udf/ext_has_spouse.py" handles tsv lines.

has_spouse_candidates :- !ext_has_spouse(ext_has_spouse_input).
ext_has_spouse_input(s, p1_id, p1_text, p2_id, p2_text) :-
  people_mentions(s, _1, _2, p1_text, p1_id),
  people_mentions(s, _3, _4, p2_text, p2_id).
```

### Feature Extraction Rules
Also, we use a UDF to extract features for the candidate relationships.

```
function ext_has_spouse_features over like ext_has_spouse_features_input
                              returns like has_spouse_features
  implementation "udf/ext_has_spouse_features.py" handles tsv lines.

has_spouse_features :- !ext_has_spouse_features(ext_has_spouse_features_input).
ext_has_spouse_features_input(ARRAY_TO_STRING(words, "~^~"), rid, p1idx, p1len, p2idx, p2len) :-
  sentences(_1, _2, words, _3, _4, _5, _6, _7, s),
  has_spouse_candidates(person1_id, person2_id, s, _8, rid, _9),
  people_mentions(s, p1idx, p1len, _10, person1_id),
  people_mentions(s, p2idx, p2len, _11, person2_id).
```


### Inference Rules
Finally, we define a binary classifier for our boolean variable `has_spouse`.

```
has_spouse(rid) :- has_spouse_candidates(_1, _2, _3, _4, rid, l)
label = l.

has_spouse(rid) :-
  has_spouse_candidates(_1, _2, _3, _4, rid, _5),
  has_spouse_features(rid, f)
weight = f
function = Imply.
```
