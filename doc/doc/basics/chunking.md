---
layout: default
---

# Example: Text chunking

## Introduction

In this document, we will describe an example application of text chunking using DeepDive, and demonstrate how to use **Multinomial variables**. This example assumes a working installation of DeepDive, and basic knowledge of how to build an application in DeepDive. Please go through the [example application walkthrough](walkthrough/walkthrough.html) before preceding.

Text chunking consists of dividing a text in syntactically correlated parts of words. For example, the sentence He reckons the current account deficit will narrow to only # 1.8 billion in September . can be divided as follows:

  [NP He ] [VP reckons ] [NP the current account deficit ] [VP will narrow ] [PP to ] [NP only # 1.8 billion ] [PP in ] [NP September ] .

Text chunking is an intermediate step towards full parsing. It is the [shared task for CoNLL-2000](http://www.cnts.ua.ac.be/conll2000/chunking/). Training and test data for this task is derived from the Wall Street Journal corpus (WSJ), which includes words, part-of-speech tags, and chunking tags.

In the example, we will predicate chunk label for each word. We include three inference rules, corresponding to logistic regression, linear-chain conditional random field (CRF), and skip-chain conditional random field. The features and rules we use are very simple, just to illustrate how to use multinomial variables and factors in DeepDive to build applications.


## Running the Example

The full example is under `examples/chunking` directory. The structure of this directory is

- `data` contains training and testing data
- `udf` contains extractor for extracting training data and features
- `result` contains evaluation scripts and sample results


To run this example, perform the following steps:

1. Run `run.sh`
2. Run `result/eval.sh` to evaluate the results


## Example Walkthrough

The application performs the following high-level steps:

1. Data preprocessing: load training and test data into database.
2. Feature extraction: extract surrounding words and their part-of-speech tags as features.
3. Statistical inference and learning
4. Evaluate results

### Data Preprocessing

The train and test data consist of words, their part-of-speech tag and the chunk tags as derived from the WSJ corpus. The raw data is first copied into into table `words_raw`, and then is processed to convert the chunk labels to integer indexes. This extractor is defined in `application.conf` using the following code:

    ext_training {
      input: "select * from words_raw"
      output_relation: "words"
      udf: ${APP_HOME}"/udf/ext_training.py"
    }


The input table `words_raw` looks like

     word_id |    word    | pos | tag  | id 
    ---------+------------+-----+------+----
           1 | Confidence | NN  | B-NP |    

The output table `words` looks like

     sent_id | word_id |    word    | pos | true_tag | tag | id 
    ---------+---------+------------+-----+----------+-----+----
           1 |       1 | Confidence | NN  | B-NP     |   0 |  0


The user-defined function `udf/ext_training.py` looks like this:

```python
#! /usr/bin/env python

# extract training data

import json, sys

tagNames = ['NP', 'VP', 'PP', 'ADJP', 'ADVP', 'SBAR', 'O', 'PRT', 'CONJP', 'INTJ', 'LST', 'B', '']
sentID = 1

# for each word
for row in sys.stdin:
  obj = json.loads(row)

  # get tag
  # extractor bug!
  if 'tag' in obj.keys():
    tag = obj['tag']
    if (tag != None and tag != 'O'): 
      tag = tag.split('-')[1]

    if tag not in tagNames: 
      tag = ''

    print json.dumps({
      'sent_id' : sentID,
      'word_id' : obj['word_id'],
      'word'    : obj['word'],
      'pos'     : obj['pos'],
      'true_tag': obj['tag'],
      'tag'     : tagNames.index(tag)
    })

  else:
    sentID += 1
    print json.dumps({
      'sent_id' : None,
      'word_id' : obj['word_id'],
      'word'    : None,
      'pos'     : None,
      'true_tag': None,
      'tag'     : tagNames.index('')
    })
```


### Feature Extraction

To predict chunking label, we need to add features. We use three simple features: the word itself, its part-of-speech tag, and the part-of-speech tag of its previous word. We add an extractor in `application.conf`

    ext_features.input: """
      select w1.word_id as "w1.word_id", w1.word as "w1.word", w1.pos as "w1.pos", 
        w2.word as "w2.word", w2.pos as "w2.pos"
      from words w1, words w2
      where w1.word_id = w2.word_id + 1 and w1.word is not null"""
    ext_features.output_relation: "word_features"
    ext_features.udf: ${APP_HOME}"/udf/ext_features.py"
    ext_features.dependencies: ["ext_training"]

where the input is generating 2-grams from `words` table, which looks like

     w1.word_id | w1.word | w1.pos | w2.word | w2.pos 
    ------------+---------+--------+---------+--------
             15 | figures | NNS    | trade   | NN

The output will look like

     word_id |   feature    | id 
    ---------+--------------+----
          15 | word=figures |   
          15 | pos=NNS      |   
          15 | prev_pos=NN  |   

The user-defined function is

```python
#! /usr/bin/env python

import json
import sys

def tostr(s):
  return '' if s is None else str(s)

# for each word
for row in sys.stdin:
  obj = json.loads(row)

  features = set()
  # sys.stderr.write(str(obj))

  # features
  w1_word = 'word=' + tostr(obj["w1.word"])
  w1_pos = 'pos=' + tostr(obj["w1.pos"])

  if 'w2.word' in obj.keys():
    w2_word = 'prev_word=' + tostr(obj["w2.word"])
    w2_pos = 'prev_pos=' + tostr(obj["w2.pos"])
  else:
    w2_word = 'prev_word='
    w2_pos = 'prev_pos='
  #w3_word = 'next_word=' + tostr(obj["words_raw.w3.word"])
  #w3_pos = 'next_pos=' + tostr(obj["words_raw.w3.pos"])

  features.add(w1_word)
  features.add(w1_pos)
  features.add(w2_pos)

  for f in features:
    print json.dumps({
      "word_id": obj["w1.word_id"],
      'feature': f
    })
```

### Statistical Learning and Inference

We will predicate the chunk tag for each word, which corresponds to `tag` column of `words` table. The variables are declared in `application.conf`:

    schema.variables {
      words.tag: Categorical(13)
    }

Here, we have 13 types of chunk tags `NP, VP, PP, ADJP, ADVP, SBAR, O, PRT, CONJP, INTJ, LST, B, null` according to CoNLL-2000 task description. We have three rules, logistic regression, linear-chain CRF, and skip-chain CRF. The logistic regression rule is

    factor_feature {
      input_query: """select words.id as "words.id", words.tag as "words.tag", word_features.feature as "feature" 
        from words, word_features 
        where words.word_id = word_features.word_id and words.word is not null"""
      function: "Multinomial(words.tag)"
      weight: "?(feature)"
    }

To express conditional random field, just use `Multinomial` factor to link variables that could interact with each other (For more information about CRF, see [this tutorial on CRF](http://people.cs.umass.edu/~mccallum/papers/crf-tutorial.pdf). The following rule links labels of neiboring words

    factor_linear_chain_crf {
      input_query: """select w1.id as "words.w1.id", w2.id as "words.w2.id", w1.tag as "words.w1.tag", w2.tag as "words.w2.tag"
        from words w1, words w2
        where w2.word_id = w1.word_id + 1"""
      function: "Multinomial(words.w1.tag, words.w2.tag)"
      weight: "?"
    }

It is similar with skip-chain CRF, where we have skip edges that link labels of identical words.

    factor_skip_chain_crf {
      input_query: """select *
      from
        (select w1.id as "words.w1.id", w2.id as "words.w2.id", w1.tag as "words.w1.tag", w2.tag as "words.w2.tag",
          row_number() over (partition by w1.id) as rn
        from words w1, words w2
        where w1.tag is not null and w1.sent_id = w2.sent_id and w1.word = w2.word and w1.id < w2.id) scrf
      where scrf.rn = 1""" 
      function: "Multinomial(words.w1.tag, words.w2.tag)"
      weight: "?"
    }

We also specify the holdout variables according to task description about training and test data.

    calibration: {
      holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM words WHERE word_id > 220663"
    }

### Evaluation Results

Running `result/eval.sh` will give the evaluation results. Below are results for using different rules. We can see that by adding CRF rules, we get better results both for precision and recall.

Logistic Regression
  processed 47377 tokens with 23852 phrases; found: 23642 phrases; correct: 19156.
  accuracy:  89.56%; precision:  81.03%; recall:  80.31%; FB1:  80.67
               ADJP: precision:  50.40%; recall:  42.92%; FB1:  46.36  373
               ADVP: precision:  69.21%; recall:  71.13%; FB1:  70.16  890
              CONJP: precision:   0.00%; recall:   0.00%; FB1:   0.00  13
               INTJ: precision: 100.00%; recall:  50.00%; FB1:  66.67  1
                LST: precision:   0.00%; recall:   0.00%; FB1:   0.00  0
                 NP: precision:  79.88%; recall:  77.52%; FB1:  78.68  12055
                 PP: precision:  90.51%; recall:  89.59%; FB1:  90.04  4762
                PRT: precision:  66.39%; recall:  76.42%; FB1:  71.05  122
               SBAR: precision:  83.51%; recall:  71.96%; FB1:  77.31  461
                 VP: precision:  79.48%; recall:  84.71%; FB1:  82.01  4965


LR + Linear-Chain CRF
  processed 47377 tokens with 23852 phrases; found: 22996 phrases; correct: 19746.
  accuracy:  91.58%; precision:  85.87%; recall:  82.79%; FB1:  84.30
                   : precision:   0.00%; recall:   0.00%; FB1:   0.00  1
               ADJP: precision:  75.74%; recall:  69.86%; FB1:  72.68  404
               ADVP: precision:  76.47%; recall:  73.56%; FB1:  74.99  833
              CONJP: precision:  25.00%; recall:  22.22%; FB1:  23.53  8
               INTJ: precision:  50.00%; recall:  50.00%; FB1:  50.00  2
                LST: precision:   0.00%; recall:   0.00%; FB1:   0.00  0
                 NP: precision:  82.22%; recall:  77.19%; FB1:  79.63  11662
                 PP: precision:  93.43%; recall:  94.26%; FB1:  93.84  4854
                PRT: precision:  66.67%; recall:  69.81%; FB1:  68.20  111
               SBAR: precision:  84.93%; recall:  74.77%; FB1:  79.52  471
                 VP: precision:  90.37%; recall:  90.21%; FB1:  90.29  4650

LR + Linear-Chain CRF + Skip-Chain CRF
  processed 47377 tokens with 23852 phrases; found: 22950 phrases; correct: 19794.
  accuracy:  91.79%; precision:  86.25%; recall:  82.99%; FB1:  84.59
                   : precision:   0.00%; recall:   0.00%; FB1:   0.00  1
               ADJP: precision:  75.25%; recall:  68.72%; FB1:  71.84  400
               ADVP: precision:  76.29%; recall:  73.56%; FB1:  74.90  835
              CONJP: precision:  30.00%; recall:  33.33%; FB1:  31.58  10
               INTJ: precision: 100.00%; recall:  50.00%; FB1:  66.67  1
                LST: precision:   0.00%; recall:   0.00%; FB1:   0.00  0
                 NP: precision:  82.96%; recall:  77.54%; FB1:  80.16  11611
                 PP: precision:  93.70%; recall:  94.30%; FB1:  94.00  4842
                PRT: precision:  66.67%; recall:  69.81%; FB1:  68.20  111
               SBAR: precision:  83.37%; recall:  74.95%; FB1:  78.94  481
                 VP: precision:  90.34%; recall:  90.34%; FB1:  90.34  4658