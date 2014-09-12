---
layout: default
---

# "Snowball sampling" Tutorial

In this document we describe how to perform *snowball sampling*, a
**systematic** technique to the quality of the results computed by DeepDive by
creating more evidence. Indeed, most of the time the information available in
existing knowledge bases is not sufficient for learning accurately, especially
as these KBs usually do not contain negative examples.

The process involves looking at the edge *weights* of the factor graph and using
this information for [distant
supervision](../general/distant_supervision.html): by analyzing the most extreme
weights, we can define new rules to label examples either positively or
negatively, increasing therefore the amount of evidence available to DeepDive to
learn the weights. Snowball sampling is therefore a *feedback* component, in
that it examines the current output of the system to act on the application in
order to improve the future outputs.

In this document we assume to have a table `gene_mentions` that contains
candidate mentions of gene and was populated by an extractor. The schema of the
table is the following:

```
   Column   |   Type    | Modifiers
------------+-----------+-----------
 id         | bigint    |
 doc_id     | text      |
 sent_id    | integer   |
 wordidxs   | integer[] |
 mention_id | text      |
 type       | text      |
 entity     | text      |
 words      | text[]    |
 is_correct | boolean   |
 features   | text[]    |
 ```

Our goal is to determine which of the candidates are indeed mentions of genes.
For each mention candidate, we have a Bernoulli (boolean) random variable
`is_correct` which we want to estimate using DeepDive. This means that in the
`schema` section of our `application.conf` file we will define a random variable
as follows:

```
schema.variables {
	# ... other variables
	gene_mentions.is_correct: Boolean
	# ... other variables
}
```

Our extractor also computed, for each candidate, a set of features that are
stored as a text array in the `features` column. Clearly, we want to use these
features to classify the mentions. We can instruct DeepDive to perform this
inference task using the following inference rule:

```
classify_gene_mentions {
	input_query: """
				SELECT
					id as "gene_mentions.id",
					is_correct as "gene_mentions.is_correct",
					unnest(features) as "gene_feature"
				FROM gene_mentions
				"""
	function: IsTrue(gene_mentions.is_correct)
	weight: "?(gene_feature)"
}
```

Note that in the last line we instructed the system to learn a different weight
for each possible feature.

Assume now that we run our application and, being not entirely satisfied with
the results, we want to increase the amount of evidence available to the system.
Snowball sampling is a systematic approach to achieve exactly this goal.

The first thing to do is to obtain a list of the features with highest and
lowest weights, using the following queries:

```sql
SELECT t1.gene_feature, t0.weight
FROM dd_inference_result_weights t0, dd_weights_classify_gene_mentions t1
WHERE t0.id = t1.id AND weight < 50 AND weight > 0 ORDER BY weight DESC LIMIT 1000;
```

This query returns a list of the 1000 features with the highest computed weight
sorted in descending ("< 50" is effectively infinite in DeepDive terms),
similar to the following:

```
         gene_feature              |      weight
-----------------------------------+------------------
 IS_HYPHENATED_SYMBOL              | 5.67192827690654
 VERB_PATH_[diagnose]              |  4.2490394982743
 IS_MAIN_SYMBOL                    | 4.24344288022672
 WINDOW_RIGHT_1_[target]           |  4.1981251927399
 IS_PXX_SYMBOL                     |  4.1622752367665
 WINDOW_RIGHT_1_[specifically]     | 4.08232353490569
 ...
```

Similarly, the following query returns the 1000 features with lowest weight:

```sql
SELECT t1.gene_feature, t0.weight
FROM dd_inference_result_weights t0, dd_weights_classify_gene_mentions t1
WHERE t0.id = t1.id AND weight > -50 AND weight < 0 ORDER BY weight ASC LIMIT 1000;
```

```
     gene_feature            |       weight
-----------------------------+--------------------
 NOT_KNOWN_ACRONYM           |  -20.3627618106166
 IS_RANDOM                   |  -18.8975495311187
 IS_ROMAN_II                 |  -17.9488104348283
 COMES_AFTER_PERSON          |  -17.2752397294184
 IS_T_CELL                   |   -17.038064954555
 IS_AFTER_DOC_ELEMENT		 |  -16.0718563023161
 IS_AFTER_INDIVIDUAL         |  -12.6434777599671
 WINDOW_RIGHT_1_[chromosome] |  -11.7503074707121
 NO_ENGLISH_WORDS_IN_SENTENCE|  -10.5329916036203
 IS_ADDRESS_SENTENCE	     |  -9.57890280680209
 ...
```

In addition to these two queries, we also want the **cardinality** of each
feature, i.e., the number of candidates containing that feature. We need this
information to discriminate whether it is worth acting on a feature: looking
only at the weights may lead to act on features used by very few examples and
therefore any action would have little impact on the performances of the system.

To obtain the feature cardinalities sorted in decreasing order we can run the
following query:

```sql
WITH feature_card AS (
	SELECT gene_feature, count(*) AS cardinality
	FROM dd_query_classify_gene_mentions
	GROUP by gene_feature)
SELECT * FROM feature_card ORDER BY cardinality DESC;
```

An example output is the following:

```
          gene_feature           | cardinality
---------------------------------+-------------
 IS_MAIN_SYMBOL                  |     5784360
 NO_ENGLISH_WORDS_IN_SENTENCE    |     2634700
 KEYWORD_SHORTEST_PATH_[cell]    |     2613047
 NOT_KNOWN_ACRONYM               |     2536788
 COMES_AFTER_PERSON              |     2481125
 ...
 ```

 At this point, we examine the features. We look for two kinds of
 features:

 - those with high weight and high cardinality which should, in theory, be
	associated mostly with *correct* gene mentions
 - those with low weight and high cardinality which should, in theory, be
	associated mostly with *incorrect* gene mentions

An example of the first kind is the `IS_MAIN_SYMBOL` feature: it represents the
fact that the candidate mentions contains the most common symbol for a gene. It
appears in many candidates, as pointed out by the huge cardinality, and it has a
high weight, suggesting  that the system will most likely classify mentions
with this feature as positive. Therefore, we can take a small leap of faith and
modify the distant supervision component of our extractor to supervise all
mention candidates with feature `IS_MAIN_SYMBOL` as positive.

An example of the second kind is the `NO_ENGLISH_WORDS_IN_SENTENCE`, which marks
the fact that the mention candidate was found in a sentence that contains no
English words, most probably an incorrect artifact of a previous step in the
pipeline to build the corpus from which to extract gene mentions. Given the low
weight associated to this feature, and the high cardinality, we can assume that
any mention candidate with this feature would be incorrect, and we can therefore
add a rule that labels mention candidates with this feature as negative.

