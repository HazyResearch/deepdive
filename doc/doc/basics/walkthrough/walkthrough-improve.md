---
layout: default
---

# Example Application: Improving the Results

This document describes how to improve the mention extractor built in [Example
Application: A Mention-Level Extraction System](walkthrough.html).

<a name="improve" href="#"> </a>

## Contents

- [Setup and First Error Analysis](#setup)
  - [Using BrainDump to generate automatic reports](#braindump)
  - [Using MindTagger to label results](#mindtagger)
- [First Error Analysis](#error-analysis-1)
  - [Fix: Use Generic Feature Library](#feature-library)
- [Second Error Analysis](#error-analysis-2)
- [Final Error Analysis](#error-analysis-3)

TODO


<!-- - [Reduce sparsity](#sparsity)
- [Use strong indicators rather than bag of words](#strong_words)
- [Add a domain-specific (symmetry) rule](#symmetry)
- [Tune sampler parameter](#tune_sampler)
- [Getting improved results](#improved_results)
 -->

Other sections in the tutorial:

- [A Mention-Level Extraction System](walkthrough.html)
- [Extras: preprocessing, NLP, pipelines, debugging extractor](walkthrough-extras.html)

## <a name="setup" href="#">  </a> Setup and First Error Analysis

### <a name="braindump" href="#"> </a> Using BrainDump to generate automatic reports

We first set up [BrainDump](https://github.com/zifeishan/braindump), the automatic report generator for DeepDive.

#### Configuration

We provide a script `./get-braindump.sh` in folder `DEEPDIVE_HOME/example/tutorial_example/step1-basic/`. Use this file to download and install BrainDump.

After installation, the first time you run `braindump`, it has an interactive command line interface to let you create a configuration file in `braindump.conf`.

The created file should be like this:

```bash

########## Conventions. Do not recommend to change. ###########

# Set the utility files dir
export UTIL_DIR="$HOME/local/braindump"

# Report folder: use current
export REPORT_DIR="$WORKING_DIR/experiment-reports"


########## User-specified configurations ###########

# Directories

# Use absolute path if possible.
# Avoid using "pwd" or "dirname $0", they don't work properly.
# $WORKING_DIR is set to be the directory where braindump is running. 
# (the directory that contains braindump.conf)
export APP_HOME=$WORKING_DIR

# Specify deepdive out directory (DEEPDIVE_HOME/out)
export DD_OUTPUT_DIR=$WORKING_DIR/../../../out

# Database Configuration
export DBNAME=deepdive_spouse
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

# Specify all feature tables. 
# e.g. FEATURE_TABLES=(f1 f2 f3)
export FEATURE_TABLES=(has_spouse_features)
export FEATURE_COLUMNS=(feature)

# Specify all variable tables
export VARIABLE_TABLES=(has_spouse)
export VARIABLE_COLUMNS=(is_true)
# Assume that in DeepDive, inference result tables will be named as [VARIABLE_TABLE]_[VARIABLE_COLUMN]_inference

# If the variable is a mention, specify the words / description for the mention. 
# This is used for a statistics with naive entity linking. If empty, do not count deduplicated mentions.
# e.g. export VARIABLE_WORDS_COLUMNS=(w1 "" w3)
# In the examples above, the second element is left empty
export VARIABLE_WORDS_COLUMNS=(description)

# Set variable docid columns to count distinct documents that have extractions
# export VARIABLE_DOCID_COLUMNS=()

# Code configs to save
export CODE_CONFIG=

# Number of samples
export NUM_SAMPLED_FEATURES=100
export NUM_SAMPLED_SUPERVISION=500
export NUM_SAMPLED_RESULT=1000
export NUM_TOP_ENTITIES=50

# Specify some tables for statistics
export SENTENCE_TABLE=sentences
export SENTENCE_TABLE_DOC_ID_COLUMN=document_id

# Define how to send result. use "true" to activate.
export SEND_RESULT_WITH_GIT=false
# If true, push after commiting the report
export SEND_RESULT_WITH_GIT_PUSH=false
export SEND_RESULT_WITH_EMAIL=false

######## CUSTOM SCRIPTS ###########
# Leave blank for default stats report.
# Set to a location of a script (e.g. $APP_HOME/your_script) to use it instead of default 

# Self-defined scripts for stats. 
export STATS_SCRIPT=
export SUPERVISION_SAMPLE_SCRIPT=
export INFERENCE_SAMPLE_SCRIPT="$APP_HOME/bdconfigs/sample-inference.sh"

########## Conventions. Do not recommend to change. ###########

# Hack: use the last DD run as output dir
# Suppose out/ is under $DEEPDIVE_HOME/
# You may need to manually change it based on need
export DD_TIMESTAMP=`ls -t $DD_OUTPUT_DIR/ | head -n 1`
export DD_THIS_OUTPUT_DIR=$DD_OUTPUT_DIR/$DD_TIMESTAMP

```

#### Run BrainDump

In `APP_HOME`, run following script to generate a automatic report for the recent DeepDive run:

```bash
braindump
```

#### Automating BrainDump with run.sh

To Automate BrainDump in `run.sh`, add following:

```bash
#! /bin/bash

. "$(dirname $0)/env.sh"

###### YOUR OTHER CONFIGURATIONS IN run.sh... ######

cd $DEEPDIVE_HOME

# Be sure to set this so that you are able to QUIT if deepdive fails.
set -e

# Run with deepdive binary:
deepdive -c $APP_HOME/application.conf

# Note that you should go back to your APP_HOME directory to run braindump
cd $APP_HOME  
braindump
```


#### Examining the results

The auto-generated report is a folder in `experiment-reports/v[xxxxx]`. Let's look into one report directory, say `experiment-reports/v[xxxxx]`:


```
# Statsitics
     number_of_documents 
    ---------------------
                     958
    (1 row)
    
     number_of_sentences 
    ---------------------
                   43789
    (1 row)
    
## Variable has_spouse
     mention_candidates 
    --------------------
                  75422
    (1 row)
    
    Supervision statistics:
     is_true | count 
    ---------+-------
             | 69486
     t       |  2164
     f       |  3772
    (3 rows)
    
     extracted_mentions 
    --------------------
                  37244
    (1 row)
    
     extracted_entities 
    --------------------
                  29465
    (1 row)
    
### Top entities
    Clinton-Obama 226
    Obama-Clinton 226
    McCain-Obama  63
    Barack Obama-Hillary Rodham Clinton 62
    Hillary Rodham Clinton-Barack Obama 62
    Obama-McCain  61
    Bill Clinton-Hillary Rodham Clinton 48
    Obama-Bill Clinton  43
    Bill Clinton-Obama  42
    Hillary Clinton-Obama 38
```

We found in "top entities" section that most entities we extract do
not make sense. To diagnose what's going on, we look at the features in 



### <a name="mindtagger" href="#"> </a> Using MindTagger to label results

We conduct error analysis based on our initial spouse relation extractor. 

For more information, see [MindTagger](../labeling.html).

Examine results TODO

We label 100 extracted relations, and the precision is only 4%.

Lessons:

- words_between is a bad feature.
  We notice that features seem to be too weak. Once there is a word
  "husband" or "wife" in the middle of two mentions, the relation is
  predicted a high probability. We may want to use stronger features.
  Also some random words get a weight, such as "word_between=Kevin" gets weight of 0.6.

- num_words_between is a feature that's too sparse. e.g. "num_words_between=17" gets a high weight.

- Lots of parallel names in a list

### Fix: Use Generic Feature Library (#feature-library)

We decide to use generic feature library.

## Second Error Analysis (#error-analysis-2)

Even worse. 1% precision.


### Fix: Add distant supervision rules (#feature-library)

negative examples.
- lots of parallel person names (>10, negative)
- "and" + no keywords
- too long

19%

## Final Error Analysis](#error-analysis-3)



select words, t0.is_true, t1.feature from sentences s, has_spouse t0, has_spouse_features t1 where t0.relation_id=t1.relation_id and s.sentence_id = t0.sentence_id and t1.feature= 'INV_NGRAM_1_[piano]';












<!-- 
### <a name="sparsity" href="#"> </a> Reduce Sparsity

After [examining the results](walkthrough.html#get_result), we noticed
that the feature `num_words_between` suffers from sparsity issues and would cause
overfitting. For example, there should be roughly no difference between having
20 and 21 words between two entity mentions. Let's change *"Feature 2"* in
`ext_has_spouse_features.py`:

```python
# Feature 2: Number of words between the two phrases
# Intuition: if they are close by, the link may be stronger.
l = len(words_between.elements)
if l < 5: features.add("few_words_between")
else: features.add("many_words_between")
```

### <a name="strong_words" href="#"></a> Use strong indicators rather than bag of words

The "bag of words" is a pretty weak feature. Our next improvement is using
strong indicators rather than a bag of words. We check the words between
two mentions to see if they are strong indicators of spouse or non-spouse
relationships, such as "marry" or "widow", or "father" "mother".

Start by modifying `application.conf` to select `lemma` as input query to
`ext_has_spouse_features`:

```bash
ext_has_spouse_features {
  input: """
    SELECT  sentences.words,
            lemma,                   # Add this line
            has_spouse.relation_id,
            p1.start_position  AS  p1_start,
            p1.length          AS  p1_length,
            p2.start_position  AS  p2_start,
            p2.length          AS  p2_length
            """
    # ...
  }
```

Then modify `ext_has_spouse_features.py` by changing *Feature 1* (bag of words)
into this feature. We still make use of `ddlib`:

```python
# Feature 1: Find out if a lemma of marry occurs.
# A better feature would ensure this is on the dependency path between the two.
words_between = ddlib.tokens_between_spans(words, span1, span2)
lemma_between = ddlib.tokens_between_spans(obj["lemma"], span1, span2)
married_words = ['marry', 'widow', 'wife', 'fiancee', 'spouse']
non_married_words = ['father', 'mother', 'brother', 'sister', 'son']
# Make sure the distance between mention pairs is not too long
if len(words_between) <= 10:         
  for mw in married_words + non_married_words:
    if mw in lemma_between.elements: 
      features.add("important_word=%s" % mw)
```


The `married_words` and `non_married_words` list can be obtained through a
"snowball-style" feature engineering: if you do not know which words to add, you
could run bag of words and check high-weight / low-weight features (via the SQL
query), and pick reasonable words to add. 

### <a name="symmetry" href="#"> </a> Add a domain-specific rule

We want to incorporate a bit of domain knowledge into our model. For example, we
know that `has_spouse` is symmetric. That means, if Barack Obama is married to
Michelle Obama, then Michelle Obama is married to Barack Obama, and vice versa.
(`Marry(A,B) <-> Marry(B,A)`) We can encode this knowledge in a second inference
rule:

```bash
    inference.factors {

      # ...(other inference rules)

      f_has_spouse_symmetry {
        input_query: """
          SELECT r1.is_true AS "has_spouse.r1.is_true",
                 r2.is_true AS "has_spouse.r2.is_true",
                 r1.id      AS "has_spouse.r1.id",
                 r2.id      AS "has_spouse.r2.id"
          FROM has_spouse r1,
               has_spouse r2
          WHERE r1.person1_id = r2.person2_id
            AND r1.person2_id = r2.person1_id
          """
        function: "Equal(has_spouse.r1.is_true, has_spouse.r2.is_true)"
        weight: "?"
      }

    }
```

There are many [other kinds of factor functions](../inference_rule_functions.html)
you could use to encode domain knowledge. 

### <a name="tune_sampler" href="#"></a> Tune sampler parameter

We can further tune sampler parameters to obtain better results. Refer to the
[sampler guide](../sampler.html) for tuning sampler parameters.

Add the following in the `deepdive` block of `application.conf`:

```bash
sampler.sampler_args: "-l 5000 -d 0.99 -s 1 -i 1000 --alpha 0.01"
```

This tells the sampler perform more sampling iterations and use a slower
step size decay. 

### <a name="improved_results" href="#"></a> Getting improved results

After performing the above modifications to extractors and inference rules, we
can run the application again and query the results:

```bash
./run.sh

psql -d deepdive_spouse -c "
  SELECT s.sentence_id, description, is_true, expectation, s.sentence
  FROM has_spouse_is_true_inference hsi, sentences s
  WHERE s.sentence_id = hsi.sentence_id and expectation > 0.95
  ORDER BY random() LIMIT 10;
"
```

The results should look like the following:

     sentence_id |       description       | is_true | expectation | sentence 
    -------------+-------------------------+---------+-------------+-------
     95331@69    | Julia Gardiner-B. Tyler |         |           1 | B. Tyler married his second wife , Julia Gardiner , in 1844 in New York City .
     114481@10   | Obama-Michelle          |         |       0.982 | And so , with those remarks , a tightly knit relationship finally came apart -- Wright had married Obama and his wife
     , Michelle , and baptized their children .
     103874@0    | Abigail-John Adams      |         |       0.982 | When John Adams begins acting like a pompous windbag , his wife , Abigail , reproaches him with a single word .
     44768@4     | Wendi-Murdoch           |         |       0.992 | Murdoch 's third wife , Wendi , is a mainland Chinese who once worked for his Hong Kong-based satellite broadcaster ,
     Star TV .
     111325@10   | Julius Rosenberg-Ethel  |         |       0.992 | Sophie Rosenberg thought Mamie Eisenhower could be a `` sympathetic ally '' in saving her son , Julius Rosenberg , an
    d his wife Ethel from execution in 1953 for espionage .
     111325@10   | Ethel-Julius Rosenberg  |         |       0.994 | Sophie Rosenberg thought Mamie Eisenhower could be a `` sympathetic ally '' in saving her son , Julius Rosenberg , an
    d his wife Ethel from execution in 1953 for espionage .
     114424@8    | Obama-Michelle          |         |       0.992 | And so , with those remarks , a tightly knit relationship finally unraveled -- Wright had married Obama and his wife
    , Michelle , and baptized their children .
     1387@16     | Rosalynn-Barbara        |         |       0.978 | Across the nave from the Ford family sat Bush and Laura Bush , and Vice President Dick Cheney , who served Ford as ch
    ief of staff , with his wife , Lynne , several current Cabinet members and three former presidents -- the elder George Bush with his wife , Barbara ; Jimmy Carter and his wife , Rosa
    lynn ; and Bill Clinton and his wife , Sen. Hillary Rodham Clinton , and their daughter Chelsea .
     119377@0    | John McCain-Cindy       |         |       0.992 | Sen. John McCain 's wife , Cindy , abruptly reversed course on Friday and released a summary of her 2006 income tax r
    eturn after weeks of vowing not to do so .
     84632@13    | Cecilia-Sarkozy         |         |       0.998 | Less than two months ago , Sarkozy and his wife , Cecilia , announced their divorce after 11 years of marriage .
    (10 rows)


Let's look at the calibration plot:

![Calibration]({{site.baseurl}}/assets/walkthrough_has_spouse_is_true_improved.png)

We should examine the learned weights again. Run the following query to select
the features with highest weights:

```bash
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_weights_mapping
  ORDER BY weight DESC
  LIMIT 5;
"
```

The results should be similar to the following:

                     description                  |      weight
    ----------------------------------------------+------------------
     f_has_spouse_features-important_word=wife    | 3.12437525600187
     f_has_spouse_features-important_word=widow   | 2.45652823047255
     f_has_spouse_features-important_word=marry   | 1.85742049055667
     f_has_spouse_features-few_words_between      |  1.6015835203787
     f_has_spouse_features-important_word=fiancee |  1.0439453467637
    (5 rows)

Run the following query to select the top negative features:

```bash
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_weights_mapping
  ORDER BY weight ASC
  LIMIT 5;
"
```

The results should be similar to the following:

                       description                   |       weight
    -------------------------------------------------+--------------------
     f_has_spouse_features-important_word=son        |  -2.83397621968201
     f_has_spouse_features-important_word=father     |  -2.76048309192415
     f_has_spouse_features-potential_last_name_match |  -2.34700944702606
     f_has_spouse_features-important_word=brother    |  -2.23063906981248
     f_has_spouse_features-important_word=sister     | -0.523695847147546
    (5 rows)

We can see that the results have been improved quite a bit, but there are still some errors. 

From the calibration plot we can tell that there are not enough features,
especially negative features. We can continue performing "snowball sampling" on
bag of words to obtain more negative features, or use better features such as
dependency paths. We can also add more negative examples by distant supervision,
or adding other domain-specific rules. To make further improvements, it is
important to conduct error analysis.

Moreover, performing entity linking and [looking for entity-level
relations](../../general/kbc.html#entity_level) is necessary for a better KBC
application.

Now if you want, you can look at the [Extras page](walkthrough-extras.html)
which explained how to prepare data tables, use pipelines, use NLP extractors,
or get example extractor inputs.

 -->