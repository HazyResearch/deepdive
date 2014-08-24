---
layout: default
---

# Example Application: Extras

This document contains the extra section for the [Example Application: A Mention-Level
Extraction System](walkthrough.html) document. 

### Contents

- [Preparing data tables for the example appliaction](#data_tables)
  - [Step-by-step preparation](#data_tables_steps)
  - [Data table format cheat-sheet](#table_cheatsheet)
- [Data preprocessing using NLP extractor](#nlp_extractor)
- [Using pipelines](#pipelines)
- [Debugging extractors by getting example inputs](#debug_extractors)

Other sections:

- [A Mention-Level Extraction System](walkthrough.html)
- [Improving the results](walkthrough-improve.html)


### <a id="data_tables" href="#"></a> Preparing Data Tables

This section explains how the `setup_database.sh` script prepares the data.

#### <a id="data_tables_steps" href="#"></a> Step-by-step data preparation

Let's start by creating a new database called `deepdive_spouse`:

```bash
createdb deepdive_spouse
```


While in the `$DEEPDIVE_HOME/app/spouse` directory, copy the `data` directory
from `$DEEPDIVE_HOME/examples/spouse_example`, if you have not done so yet:

```bash
cp -r ../../examples/spouse_example/data .
```

The `data` folder contains several text dumps:

- `articles_dump.csv` contains initial data: articles we extract relation from.
  We will just start from the parsed sentences dataset:
- `sentences_dump.csv` contains all parsed sentences from these articles. If you
  want to know how to get this dataset from articles, refer to [NLP
  extractor](#nlp_extractor) section.
- `spouses.csv` and `non-spouses.tsv` Freebase relations we will use for distant
  supervision. We will come to them later.

First we create a `sentences` table in our database: 

```bash
psql -d deepdive_spouse -c "
  CREATE TABLE sentences(
    document_id  bigint,    -- which document it comes from
    sentence     text,      -- sentence content
    words        text[],    -- array of words in this sentence
    lemma        text[],    -- array of lemmatized words
    pos_tags     text[],    -- array of part-of-speech tags
    dependencies text[],    -- array of dependency paths
    ner_tags     text[],    -- array of named entity tags (PERSON, LOCATION, etc)
    sentence_offset bigint, -- which sentence (0, 1, 2...) is it in document
    sentence_id text        -- unique identifier for sentences
    );
"
```

Then we load the prepared sentences into our database:

```bash
psql -d deepdive_spouse -c "
  COPY sentences FROM STDIN CSV;
" < ./data/sentences_dump.csv
```

Here we go! We have all sentences prepared in our database.

Then we create several empty tables for the feature extraction output. 

First we create `people_mentions` table for people mention pairs:
  
```bash
psql -d deepdive_spouse -c "
  CREATE TABLE people_mentions(
    sentence_id    text,   -- refers to sentences table
    start_position int,    -- word offset in the sentence
    length         int,    -- how many words in this mention
    text           text,   -- name of the person
    mention_id     text    -- unique identifier for people_mentions
  );
"
```

Then we create a table `has_spouse` for our relations:

```bash
psql -d deepdive_spouse -c "
  CREATE TABLE has_spouse(
    person1_id  text,    -- first person's mention_id in people_mentions
    person2_id  text,    -- second person's mention_id
    sentence_id text,    -- which senence it appears
    description text,    -- a description of this relation pair
    is_true     boolean, -- whether this relation is correct
    relation_id text,    -- unique identifier for has_spouse
    id          bigint   -- reserved for DeepDive
  );
"
```

Then we create the feature table for all relations:

```bash
psql -d deepdive_spouse -c "
  CREATE TABLE has_spouse_features(
    relation_id text,    -- refers to has_spouse.relation_id
    feature     text     -- feature content (e.g. words_between=wife of)
  );
"
```

We are done, all the tables have been created.


#### <a id="table_cheatsheet" href="#"></a> Data table format Cheat-sheet

The following relations are present in the database:

                         List of relations
     Schema |        Name         | Type  |  Owner   | Storage
    --------+---------------------+-------+----------+---------
     public | articles            | table | deepdive | heap
     public | has_spouse          | table | deepdive | heap
     public | has_spouse_features | table | deepdive | heap
     public | people_mentions     | table | deepdive | heap
     public | sentences           | table | deepdive | heap
    (5 rows)

<a name="tables" href="#"></a>
This is how they are created:

```sql
CREATE TABLE articles(
  article_id bigint,      -- identifier of article
  text       text         -- all text in the article
);
CREATE TABLE sentences(
  document_id  bigint,    -- which document it comes from
  sentence     text,      -- sentence content
  words        text[],    -- array of words in this sentence
  lemma        text[],    -- array of lemmatized words
  pos_tags     text[],    -- array of part-of-speech tags
  dependencies text[],    -- array of dependency paths
  ner_tags     text[],    -- array of named entity tags (PERSON, LOCATION, etc)
  sentence_offset bigint, -- which sentence (0, 1, 2...) is it in document
  sentence_id  text       -- unique identifier for sentences
  );
CREATE TABLE people_mentions(
  sentence_id    text,    -- refers to sentences table
  start_position int,     -- word offset in the sentence
  length         int,     -- how many words in this mention
  text           text,    -- name of the person
  mention_id     text     -- unique identifier for people_mentions
);
CREATE TABLE has_spouse(
  person1_id  text,       -- first person's mention_id in people_mentions
  person2_id  text,       -- second person's mention_id
  sentence_id text,       -- which senence it appears
  description text,       -- a description of this relation pair
  is_true     boolean,    -- whether this relation is correct
  relation_id text,       -- unique identifier for has_spouse
  id          bigint      -- reserved for DeepDive
);
CREATE TABLE has_spouse_features(
  relation_id text,       -- refers to has_spouse.relation_id
  feature     text        -- feature content (e.g. "words_between='s wife")
);
```

### <a id="nlp_extractor" href="#"> </a> Data preprocessing using NLP extractor

If you want, you can try extracting the `sentences` table yourself. This should
be useful if you want to extract your own dataset.

To start from an NLP extractor, we first load all the articles into our
database. We start by creating a table:

```bash
psql -d deepdive_spouse -c "
  CREATE TABLE articles(
    article_id bigint,    -- identifier of article
    text       text       -- all text in the article
  );
"
```

Then we copy all the articles from the dump:

```bash
psql -d deepdive_spouse -c "
  COPY articles FROM STDIN CSV;
" < data/articles_dump.csv
```


The first step towards performing entity and relation extraction is to extract
natural language features from the raw text. This is usually done using an NLP
library such as [the Stanford
Parser](http://nlp.stanford.edu/software/lex-parser.shtml) or
[NLTK](http://nltk.org/). Because natural language processing is such a common
first step we provide a pre-built extractor which uses the [Stanford CoreNLP
Kit](http://nlp.stanford.edu/software/corenlp.shtml) to split documents into
sentences and tag them. Let's copy it into our project. 

The NLP extractor we provide lies in `examples/nlp_extractor` folder. Refer to
its `README.md` for details. Now we go into it and compile it:

```bash  
cd ../../examples/nlp_extractor
sbt stage
cd ../../app/spouse
```

The `sbt stage` command compiles the extractor (written in Scala) and generates
a handy start script. The extractor itself takes JSON tuples of raw document
text as input, and outputs JSON tuples of sentences with information such as
part-of-speech tags and dependency parses. We now create a new table for the
output of the extractor in the database. Since the output format of the NLP
extractor is fixed, we must create a compatible table, like the `sentences`
table defined [above](#tables).

Next, we add the extractor: 

```bash
extraction.extractors {

  ext_sentences: {
    input: """
      SELECT  article_id, text
      FROM    articles
      ORDER BY article_id ASC
      """
    output_relation : "sentences"
    udf             : "examples/nlp_extractor/run.sh -k article_id -v text -l 100 -t 4"
    before          : ${APP_HOME}"/udf/before_sentences.sh"
    after           : ${APP_HOME}"/util/fill_sequence.sh sentences sentence_id"
    parallelism     : 4
  }
  # ... More extractors to add here
}
```

(Make sure this extractor is executed before `ext_people` by adding dependencies to the latter.)

<a id="pipelines" href="#"> </a>

### Using pipelines

By default, DeepDive runs all extractors that are defined in the configuration
file. Sometimes you only want to run some of your extractors to test them, or to
save time when the output of an early extractor hasn't changed. The NLP
extractor is a good example of this. It takes a long time to run, and its output
will be the same every time, so we don't want to run it more than once. DeepDive
allows you to define different [pipelines](../running.html#pipelines) for this purpose, by
adding the following to your `application.conf`:

```bash
pipeline.pipelines.withnlp: [
  "ext_sentences",    # NLP extractor, takes very long
  "ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features",
  "f_has_spouse_features", "f_has_spouse_symmetry"
]

pipeline.pipelines.nonlp: [
  "ext_people", "ext_has_spouse_candidates", "ext_has_spouse_features",
  "f_has_spouse_features", "f_has_spouse_symmetry"
]
```

The code above created two pipelines, one with NLP extraction and the other
without NLP. We further add a line:

```bash
pipeline.run: "nonlp"
```

This directive instructs DeepDive to execute the "nonlp" pipeline, which only
contains the "ext_people" extractor.

<a id="debug_extractors" href="#"> </a>

### Debugging Extractors by getting example inputs

"What do my extractor inputs look like?" Developers might find it helpful to
print input to extractors to some temporary files. For a general instruction to debug extractors, refer to [debugging section in extractor documentation](../extractors.html#debug_extractors).


