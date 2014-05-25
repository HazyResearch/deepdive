---
layout: default
---

# Example Application: Extras

This page is the extra section for [Example Application: A Mention-Level Extraction System](walkthrough-mention.html). 

### Contents

- [Preparing data tables for the example appliaction](#data_tables)
  - [Step-by-step preparation](#data_tables_steps)
  - [Data table format cheat-sheet](#table_cheatsheet)
- [Data preprocessing using NLP extractor](#nlp_extractor)
- [Using pipelines](#pipelines)
- [Debugging extractors by getting example inputs](#debug_extractors)

Other sections:

- [Walkthrough](walkthrough.html)
- [A Mention-Level Extraction System](walkthrough-mention.html)
- [Improving the results](walkthrough-improve.html)


<a id="data_tables" href="#"> </a>

### Preparing Data Tables

If you want to know how `setup_database.sh` prepares the data, here is the detailed guide.

<a id="data_tables_steps" href="#"> </a>

#### Step-by-step data preparation

Let's start by creating a new database called `deepdive_spouse` by typing in command line:

```bash
createdb deepdive_spouse
```


Make sure you are currently under `$DEEPDIVE_HOME/app/spouse` folder. Make sure to copy `data` folder from `$DEEPDIVE_HOME/examples/spouse_example`, if you haven't done so:

```bash
cp -r ../../examples/spouse_example/data .
```

The `data` folder contains several starter dumps:

- `articles_dump.csv` contains initial data: articles we extract relation from. We will just start from the parsed sentences dataset:
- `sentences_dump.csv` contains all parsed sentences from these articles. If you want to know how to get this dataset from articles, refer to [NLP extractor](#nlp_extractor) section.
- `spouses.csv` and `non-spouses.tsv` Freebase relations we will use for distant supervision. We will come to them later.

First we create a `sentences` table in our database by typing: 

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

Then we load prepared sentences into our database:

```bash
psql -d deepdive_spouse -c "
  COPY sentences FROM STDIN CSV;
" < ./data/sentences_dump.csv
```

Here we go! We have all sentences prepared in our database.

Then we create several empty tables for feature extraction output. 

First create `people_mentions` table for people mention pairs, by typing:
  
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


Then create a table `has_spouse` for our relations by typing:

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

Then create the feature table for all relations:

```bash
psql -d deepdive_spouse -c "
  CREATE TABLE has_spouse_features(
    relation_id text,    -- refers to has_spouse.relation_id
    feature     text     -- feature content (e.g. words_between=wife of)
  );
"
```


<a id="table_cheatsheet" href="#"> </a>

#### Data table format Cheat-sheet

Following relations are provided in the database dump:

                         List of relations
     Schema |        Name         | Type  |  Owner   | Storage
    --------+---------------------+-------+----------+---------
     public | articles            | table | deepdive | heap
     public | has_spouse          | table | deepdive | heap
     public | has_spouse_features | table | deepdive | heap
     public | people_mentions     | table | deepdive | heap
     public | sentences           | table | deepdive | heap
    (5 rows)

Here's how they are created:

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

<a id="nlp_extractor" href="#"> </a>

### Data preprocessing using NLP extractor

If you want, you can try extracting the `sentences` table yourself. This should be useful if you want to extract your own dataset.

To start from an NLP extractor, we first load all the articles into our database. First, let's load `articles` table into our database.

Type in following commands to create a table:

```bash
psql -d deepdive_spouse -c "
  CREATE TABLE articles(
    article_id bigint,    -- identifier of article
    text       text       -- all text in the article
  );
"
```

Then copy all the articles from the dump:

```bash
psql -d deepdive_spouse -c "
  COPY articles FROM STDIN CSV;
" < data/articles_dump.csv
```


The first step towards performing Entity and Relation Extraction is to extract natural language features from the raw text. This is usually done using an NLP library such as [the Stanford Parser](http://nlp.stanford.edu/software/lex-parser.shtml) or [NLTK](http://nltk.org/). Because natural language processing is such a common first step we provide a pre-built extractor which uses the [Stanford CoreNLP Kit](http://nlp.stanford.edu/software/corenlp.shtml) to split documents into sentences and tag them. Let's copy it into our project. 

The NLP extractor we provide lies in `examples/nlp_extractor` folder. Refer to its `README.md` for details. Now we go into it and compile it:

```bash  
cd ../../examples/nlp_extractor
sbt stage
cd ../../app/spouse
```

The `sbt stage` command compiles the extractor (written in Scala) and generates a handy start script. The extractor itself takes JSON tuples of raw document text as input, and outputs JSON tuples of sentences with information such as part-of-speech tags and dependency parses. Let's now create a new table for the output of the extractor in our database. Because the output format of the NLP extractor is fixed by us, you must create a compatible table, like `sentences` defined [above](#loading_data).

Next, add the extractor: 

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
  # ... More extractors to add dere
}
```

(Make sure this extractor is executed before `ext_people` by adding dependencies to the latter.)


<a id="pipelines" href="#"> </a>

### Using pipelines

By default, DeepDive runs all extractors that are defined in the configuration file. Sometimes you only want to run some of your extractors to test them, or to save time when the output of an early extractor hasn't changed. The NLP extractor is a good example of this. It takes a long time to run, and its output will be the same every time, so we don't want to run it more than once. DeepDive allows you to define different [pipelines](pipelines.html) for this purpose, by adding the following to your `application.conf`:

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

The code above created two pipelines, one with NLP extraction and the other without NLP. We further add a line:

```bash
pipeline.run: "nonlp"
```

This will tell DeepDive to execute the "nonlp" pipeline, which only contains the "ext_people" extractor.

<a id="debug_extractors" href="#"> </a>

### Debugging Extractors by getting example inputs

"What do my extractor inputs look like?" Developers might find it helpful to print input to extractors to some temporary files. DeepDive provides a simple utility script for this task, in `$DEEPDIVE_HOME/util/extractor_input_writer.py`. The script is very simple:

```python
#! /usr/bin/env python
# File: deepdive/util/extractor_input_writer.py

# Simply printing input lines to a file, specified by a command line argument.
import sys
if len(sys.argv) != 2:
  print >>sys.stderr, "Usage:", sys.argv[0], "SAMPLE_FILE_PATH"
  sys.exit(1)

fout = open(sys.argv[1], 'w')
print >>sys.stderr, "Writing extractor input to file:",sys.argv[1]
for line in sys.stdin:
  print >>fout, line.rstrip('\n')

fout.close()
```

This script takes one command line argument: file output path. It will simply output whatever it receives as input from STDIN to the file.

Developers can change extractor UDF to `util/extractor_input_writer.py SAMLPE_FILE_PATH` to obtain example extractor inputs in file `SAMLPE_FILE_PATH`.

For example, to debug the extractor `ext_has_spouse_features`, just change `application.conf` to:
 
 ```bash
ext_has_spouse_features {
  # Added "ORDER BY" and "LIMIT" to randomly sample a small amount of data
  input: """
    SELECT  sentences.words,
            has_spouse.relation_id, 
            p1.start_position AS p1_start,
            p1.length AS p1_length,
            p2.start_position AS p2_start,
            p2.length AS p2_length
      FROM  has_spouse, 
            people_mentions p1, 
            people_mentions p2, 
            sentences
     WHERE  has_spouse.person1_id = p1.mention_id 
       AND  has_spouse.person2_id = p2.mention_id 
       AND  has_spouse.sentence_id = sentences.sentence_id
       ORDER BY RANDOM() LIMIT 100
       """
  output_relation : "has_spouse_features"
  # udf: ${APP_HOME}"/udf/ext_has_spouse_features.py"     # Comment it out

  # Change UDF to the utility file; save outputs to "/tmp/dd-sample-features.txt".
  # "util" folder is under DEEPDIVE_HOME.
  udf: util/extractor_input_writer.py /tmp/dd-sample-features.txt

  before          : ${APP_HOME}"/udf/clear_table.sh has_spouse_features"
  dependencies    : ["ext_has_spouse_candidates"]
}
```

After running the system with `run.sh`, the file `/tmp/dd-sample-features.txt` look like:


    {"p2_length":2,"p1_length":2,"words":["The","strange","case","of","the","death","of","'50s","TV","Superman","George","Reeves","is","deconstructed","in","``","Hollywoodland",",","''","starring","Adrien","Brody",",","Diane","Lane",",","Ben","Affleck","and","Bob","Hoskins","."],"relation_id":12190,"p1_start":20,"p2_start":10}
    {"p2_length":2,"p1_length":2,"words":["Political","coverage","has","not","been","the","same","since","The","National","Enquirer","published","photographs","of","Donna","Rice","in","the","former","Sen.","Gary","Hart","'s","lap","20","years","ago","."],"relation_id":34885,"p1_start":14,"p2_start":20}
    ...

We see that each line contains a JSON object. And you can even use this file to test your extractor UDF by typing commands like this:

```bash
python udf/ext_has_spouse_features.py < /tmp/dd-sample-features.txt
```
