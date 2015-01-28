# NLP Extractor

This directory provides an NLP extractor that is a wrapper for
Stanford NLP. Its input is textual data in TSV format, and its output
is one or many  TSV files that contains processed sentences data that
can be directly loaded into PostgreSQL / Greenplum tables.

In (1) we show how to use it this NLP extractor as an "extractor" in
DeepDive, and in (2) we provide a script to run it as a stand-alone
application in parallel, without DeepDive --- which is a recommended
way if you have lots of data, since this way can achieve more
parallism.

## Compile

Compile the NLP extractor using following command in this directory:

    sbt stage

## (1) run.sh and Integration with DeepDive

### Input

The input file is a TSV file of following form. Each line is a
document.

The TSV file should have two columns. The first column is
`document_id` (text format), a unique modifier for a document. The
second column is `text`, all sentences in the document in plain text:

    doc1\tFull text in doc1
    doc2\tFull text in doc2
    ...

Note that the input TSV file should not have headers.

### Output

Output is another TSV file that contains multiple columns:

1. `document_id`: The document_id from input.
2. `sentence`: The raw sentence text same as input
3. `words`: (PSQL-formatted) array of words
4. `lemma`: array of lemmatized words
5. `post_tags`: array of Part-of-speech tags
6. `ner_tags`: array of Named Entity tags
7. `dependencies`: array of collapsed dependencies
8. `sentence_offset`: The index / offset of this sentence in document
9. `sentence_id`: A unique identifier to the sentence

You can create a table like this to be able to import the output TSV
file to the database. (Note that this is the `output_relation` in
DeepDive)

    CREATE TABLE sentences(
      document_id bigint,
      sentence text, 
      words text[],
      lemma text[],
      pos_tags text[],
      dependencies text[],
      ner_tags text[],
      sentence_offset bigint,
      sentence_id text
      );

## (2) run_parallel.sh: Stand-alone Parallel NLP Extractor

When used the `run.sh` with DeepDive, sometime ideal parallelism
cannot be achieved because of memory problems. In this case, we
recommend to use the `run_parallel.sh`. It does the following steps:

1. Split your input file `INPUT_FILE` into chunks in `INPUT_FILE.split/`
2. Uses system parallelism tool `xargs` to run `run.sh` in parallel. 
   The outputs are saved to `INPUT_FILE.split/*.out`.

Run it with the following command

    ./run_parallel.sh INPUT_FILE PARALLELISM [INPUT_BATCH_SIZE=100] [SENTENCE_WORDS_LIMIT=120]

- `INPUT_FILE`: your input TSV file
- `PARALLELISM`: a number indicating desired parallelism. e.g.: 8
- `INPUT_BATCH_SIZE`: how many lines are in each file after split.
  Default 100.
- `SENTENCE_WORDS_LIMIT`: Do not run dependency parsing if number of
  words in sentence is larger than this number. This helps in speeding
  up the parsing.

When finished, you should manually import the files in 
`INPUT_FILE.split/*.out` into your database. You can use a COPY query
like this:

    cat INPUT_FILE.split/*.out | psql YOUR_DB_NAME -c "COPY sentences FROM STDIN"
