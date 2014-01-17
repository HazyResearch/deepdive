---
layout: default
---

# Overview

To illustrate a more concrete application of DeepDive, we provide a simple entity-linking example, located in examples/kbp. This example is taken from the [KBP entity linking competition](http://www.nist.gov/tac/2013/KBP/EntityLinking/). The objective of the competition is to extract from a text data corpus mentions labeled PERSON, ORGANIZATION, and GPE (geo-political entity), and to link these mentions to known entities with the same labels. To perform the linking, a training set is used to learn the association between the different (entity, mention) pairs, and an evaluation set is used to evaluate system performance.

For illustrative purposes, we will just extract the PERSON mentions from raw text and link them to PERSON entities found in Freebase. In the example, we provide a partial Freebase dump file for entity extraction, and a small set of Newswire documents for mention extraction.

DeepDive extracts features from the entity and mention text, and then performs probabilistic inference on the (entity, mention) variables.

At a high level, the following tasks are performed:
1. Data processing/loading
2. Candidate link extraction
3. Feature extraction
4. Inference
5. Evaluation


### Dependencies

In addition to the DeepDive dependencies, you will need the following libraries to run this example:
* [BeautfulSoup]() for processing raw Newswire XML
* [NLTK]() for named-entity recognition to extract mentions


### 1. Data Processing/Loading

For the complete extract-train-infer pipeline, the following data is provided (located in examples/kbp/data):

- Raw source data for mentions (Newswire documents)

```
[sample line]
```

- Entity data (subset of [Freebase dump](https://developers.google.com/freebase/data))

```
[sample line]
```

- Training data (artificially constructed for this example)

```
[sample line]
```

- Query data (what entity-mention links we are trying to predict)

```
[sample line]
```

- Evaluation data (artificially constructed for this example)

```
[sample line]
```

Data processing steps:

- Raw entity data is converted to CSV using generate_csv.py.
 - The Freebase dump file contains (subject, predicate, object) triples in RDF format.
 - generate_csv.py processes each triple, extracts the English name for the ones where the object is PERSON, and writes the processed data to a CSV file.

- Raw mention data is converted to CSV using generate_csv.py.
 - To extract possible PERSON mentions, named-entity recognition is done using [NLTK](http://nltk.org/). For example, in the sentence "Barack Obama is the president" NLTK would recognize that Barack Obama is a PERSON.
 - generate_csv.py extracts the article text from the <text> tag, performs entity recognition on the text, and outputs potential PERSON instances to a CSV file.

- The CSV entity and mention data is loaded into the Postgres database with prepare_data.sh.


### 2. Candidate Link Extraction

We want to predict whether the "Barack Obama" mention should link to the "Barack Obama" entity. First, we find potential candidate matches between entities and mentions. What qualifies as a candidate match will depend on metrics that tells us how similar the entity and mention in an (e, m) pair are. For a given metric we want a binary output indicating whether the pair is a candidate match.

Candidate (entity, mention) pairs are found by performing a theta join on the entity and mention tables, using the text attributes as inputs to the predicate. The example uses the following predicates, implemented in SQL:
- Exact string match
- Similarity score above a threshold of 0.75
- Levenshtein edit distance below 3

Candidate (e, m) tuples must be distinct, so after populating the candidate table a view is created to extract the unique (e, m) pairs from all candidates. 

### 3. Feature extraction

Once the candidate (entity, mention) pairs are found, feature extraction is performed for each candidate (e, m) pair. Features are binary indicators of whether certain predicates are true. The output of this step is the link_features relation: link_feature(entity_id, mention_id, feature_type) where feature_type is a string name representing a particular feature (e.g. exact_string_match). 

 The example uses the following features:
- Exact string match
- Similarity score above a threshold of 0.75
- Levenshtein edit distance below 3

Note that the features are the same as the predicates that are used to determine if a pair is a candidate link or not.

For example, for the (e, m) pair with the text (Barack Obama, Barack Obama), all 3 features would be True and thus 3 tuples would be generated in the link_feature relation: (e, m, 'exact_string_match'), (e, m, 'similarity_above_threshold', 'levenshtein_distance_below_3').

```
[insert extractor code from application.conf with explanation]
```

### 4. Inference

DeepDive models entity linking as a factor graph where the variables are (entity, mention) pairs with corresponding True/False values, factors are features extracted between candidate entity-mention pairs, and the objective is to learn factor weights from training data and infer values for all uknown variables in the query data.

As outlined in application.conf, our variables are unique (entity, mention) pairs such that a given mention only links to one entity.

The factors are as follows:
- Feature type (e.g. edit distance, etc.)
- 1-to-many constraint between entities and mentions

```
[code snippet]
```

### 5. Evaluation

// TODO
