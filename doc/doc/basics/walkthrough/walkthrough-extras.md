---
layout: default
---

# Example Application: Extras

This document contains the extra section for the [Example Application: A Mention-Level
Extraction System](walkthrough.html) document. 

### Contents

- [Preparing data tables](#data_tables)
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

First, the script `setup_database.sh` checks if all data files exist in the `$APP_HOME` folder. You should have [downloaded the archive](http://i.stanford.edu/hazy/deepdive-tutorial-data.zip) and extracted the files to `data/` directory.

The `data` folder contains several text dumps:

- `spouses.tsv` and `non-spouses.tsv` contains Freebase relations we use for distant
  supervision.
- `sentences_dump.csv` contains a dataset of parsed sentences. If you
  want to know how to get this dataset from articles, refer to [NLP
  extractor](#nlp_extractor) section.
- `sentences_dump_large.csv` contains a larger dataset of parsed sentences used in [Improving the results: adding more data](walkthrough-improve.html#error-analysis-2) section.


Then, the scripts creates a new database called `deepdive_spouse`:

```bash
createdb deepdive_spouse
```

It then creates all tables specified in `$APP_HOME/schema.sql`. 
Refer to the [table format cheat-sheet](#table_cheatsheet) below.

Finally, it loads the prepared sentences into our database:

```bash
psql -d $DBNAME -c "copy sentences from STDIN CSV;" < $APP_HOME/data/sentences_dump.csv
```

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

When you start your own application, you usually start with your own dataset containing raw text. 
The first step towards performing entity and relation extraction is to extract
natural language features from the raw text. This is usually done using an NLP
library such as [the Stanford
Parser](http://nlp.stanford.edu/software/lex-parser.shtml) or
[NLTK](http://nltk.org/). Because natural language processing is such a common
first step, DeepDive provides a pre-built extractor which uses the 
[Stanford CoreNLP Kit](http://nlp.stanford.edu/software/corenlp.shtml) 
to split documents into sentences and tag them. 

One instance of output of the NLP extractor is the `sentences` table used in 
[the tutorial](walkthrough.html). 

To start using an NLP extractor, we first load all your articles into our
database. We start by creating a table:

```bash
psql -d deepdive_spouse -c "
  CREATE TABLE articles(
    article_id bigint,    -- identifier of article
    text       text       -- all text in the article
  );
"
```

Then we copy all the articles from your data. As an example, you can try loading with the dump in `DEEPDIVE_HOME/examples/spouse_example/data/articles_dump.csv`:

```bash
psql -d deepdive_spouse -c "
  COPY articles FROM STDIN CSV;
" < $DEEPDIVE_HOME/examples/spouse_example/data/articles_dump.csv
```

The NLP extractor is in `examples/nlp_extractor` folder. Refer to
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

Next, we add the extractor into your `application.conf`: 

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
  }
  # ... More extractors to add here
}
```

<!-- TODO before          : ${APP_HOME}"/udf/before_sentences.sh"
    after           : ${APP_HOME}"/util/fill_sequence.sh sentences sentence_id"
 -->    

When running your application, this extractor will run NLP over all the sentences in your `articles` table and output the parsed results into `sentences` table.

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
  "ext_people", "ext_has_spouse", "ext_has_spouse_features",
  "f_has_spouse_features"
]

pipeline.pipelines.nonlp: [
  "ext_people", "ext_has_spouse", "ext_has_spouse_features",
  "f_has_spouse_features"
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


