---
layout: default
---

# Building an Incremental Application

This document describes how to build an incremental application using ddlog and
Deepdive. The example application is from spouse example.

This document assumes you are familiar with basic concepts in DeepDive and the
spouse application tutorial.

## Incremental application workflow

In building knowledge base construction, a typical scenario is the user has
already built a knowledge base application, and wants to try new features or add
new data. We call the base case materialization phase, and the iterative process
of adding features or data incremental phase. The user will run materialization
phase to  materialize the base factor graph, and can run multiple incremental
phase on top of the base factor  graph. Each incremental run is independent, and
is relative to the base run. Once the user is satisfied with the incremental
experiments, the user can run  merge phase to merge the incremental part into
the base part. The workflow can be summaried as follows.

1. Preprocess and load data
2. Write the ddlog program
3. Run materialization phase to materialize the base factor graph
4. Add features, or add new data
5. Run incremental phase
6. Repeat 4 and 5 until satisfied
7. Run merge phase to merge the incremental part into the base part


### Preprocess and load data

This walkthrough is based on tsv-extractor spouse example under
deepdive/examples/spouse_example/tsv_extractor. First, copy the whole folder
into the app folder where we want to put our application files. Then, follow the
corresponding section in the spouse example tutorial to load the data. Note all
the arrays should be transformed into string using array_to_string with delimiter
'~^~'.


### Write ddlog application

We can rewrite the spouse example in ddlog. Please refer to ddlog tutorial about
how to  write a ddlog program. The ddlog spouse example is as follows

  articles(
    article_id text,
    text       text).

  sentences(
    document_id     text,
    sentence        text,
    words           text,
    lemma           text,
    pos_tags        text,
    dependencies    text,
    ner_tags        text,
    sentence_offset int,
    sentence_id     text).

  people_mentions(
    sentence_id    text,
    start_position int,
    length         int,
    text           text,
    mention_id     text).

  has_spouse_candidates(
    person1_id  text,
    person2_id  text,
    sentence_id text,
    description text,
    relation_id text,
    is_true boolean).

  has_spouse_features(
    relation_id text,
    feature     text).

  has_spouse?(relation_id text).

  people_mentions :-
    !ext_people(ext_people_input).

  ext_people_input(s, words, ner_tags) :-
    sentences(a, b, words, c, d, e, ner_tags, f, s).

  function ext_people over like ext_people_input
                   returns like people_mentions
    implementation "udf/ext_people.py" handles tsv lines.

  has_spouse_candidates :-
    !ext_has_spouse(ext_has_spouse_input).

  ext_has_spouse_input(s, p1_id, p1_text, p2_id, p2_text) :-
    people_mentions(s, a, b, p1_text, p1_id),
    people_mentions(s, c, d, p2_text, p2_id).

  function ext_has_spouse over like ext_has_spouse_input
                       returns like has_spouse_candidates
    implementation "udf/ext_has_spouse.py" handles tsv lines.

  has_spouse_features :-
    !ext_has_spouse_features(ext_has_spouse_features_input).

  ext_has_spouse_features_input(words, rid, p1idx, p1len, p2idx, p2len) :-
    sentences(a, b, words, c, d, e, f, g, s),
    has_spouse_candidates(person1_id, person2_id, s, h, rid, x),
    people_mentions(s, p1idx, p1len, k, person1_id),
    people_mentions(s, p2idx, p2len, l, person2_id).
    
  function ext_has_spouse_features over like ext_has_spouse_features_input
                                returns like has_spouse_features
    implementation "udf/ext_has_spouse_features.py" handles tsv lines.

  has_spouse(rid) :- has_spouse_candidates(a, b, c, d, rid, l) label = l.

  has_spouse(rid) :-
    has_spouse_candidates(a, b, c, d, rid, l),
    has_spouse_features(rid, f)
  weight = f.

We put it under the application folder, called spouse.ddl.

### Materialization phase

In this phase, we will compile the ddlog program into application.conf, and then
run the application. 

In this walkthrough, we will demostrate incremental application by adding new
features. In the udf `ext_has_spouse_features.py`, we'll use basic feature 1 as
for materialization phase, and feature 2 for incremental phase. 

Suppose we have the ddlog compiler ddlog.jar under current
folder. Compile the ddlog using the following command

```bash
java -jar ddlog.jar compile --materialization spouse.ddl > application.conf
```

After compiling ddlog to application.conf, we can run the application by setting 
up a pipeline and run `run.sh`. First do

```bash
export PIPELINE=initdb
sh run.sh
```

to initialize the database. Then do

```bash
export PIPELINE=extraction
sh run.sh
export PIPELINE=inference
sh run.sh
```

to run extraction and inference. We will get a materialized base factor graph under
`deepdive/out/[timestamp]` folder.

We also need to indicate the active variables/factors/weights. We call variables
in the relations that the user wants to improve active variables, and we call
inactive variables those that are not needed in the next KBC iteration. Similarly
for factors and weights. These are generated using deepdive/util/active.sh.
There are db connection and folder settings in this file. The output folder should
be the same output folder as above. We can fill in the active variable relation
name and the active inference rule name. These names can be found in application.conf.
In our example, active variable relation would be `has_spouse`. Active inference
rule would be `has_spouse_0`.

We can run the sampler by using the following command

```bash
[sampler] mat -i 100 -s 100 -l 100 -r data/base/
```

where [sampler] is the sampler binary. The materialized result is stored in 
`data/base`.

### Incremental phase

In this phase, we will update the application by adding a new feature for each
relation candidate, and run the application. 

In `ext_has_spouse_features.py`, we use feature 2. Then, compile the ddlog using
the following command

```bash
java -jar ddlog.jar compile --incremental spouse.ddl > application.conf
```

After compilation, we can run the application. First do

```bash
export PIPELINE=initdb
sh run.sh
```

to initialize the database. Then do

```bash
export PIPELINE=extraction
sh run.sh
export PIPELINE=inference
sh run.sh
```

We will get an incremental factor graph also in `deepdive/out/[timestamp]` folder.

We can run sampler using

```bash
[sampler] inc -i 100 -s 100 -l 100 -r data/base/ -j data/inc/
```
The incremental result will be stored in `data/inc/`. The above incremental phase
can be repeated for several times until we are satisfied with our result. When
running another incremental run, instead of using `export PIPELINE=initdb`,
we will use `export PIPELINE=cleanup` before each run.


### Merge phase

Once we decide to keep the current version, we can merge the incremental part into
the base by compiling the ddlog using `--merge` flag:

```bash
java -jar ddlog.jar compile --merge spouse.ddl > application.conf
```

The run `run.sh`. Till here, we finish a full workflow of incremental application.
