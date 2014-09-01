---
layout: default
---

# Relation Extraction

**Relation extraction** helps extracting **structured information** from
unstructured sources such as raw text. One may want to find interactions between
drugs to build a medical database, understand the scenes in images, or extract
relationships among people to build an easily searchable knowledge base. 

For example, let's assume we are interested in marriage relationships. We want
to automatically figure out that "Michelle Obama" is the wife of "Barack Obama"
from a corpus of raw text snippets such as "Barack Obama married Michelle Obama
in...". A naive approach would be to search news articles for indicative
phrases, like "married" or "XXX's spouse". This will yield some results, but
human language is inherently ambiguous, and one cannot possibly come up with all
phrases that indicate a marriage relationship. A natural next step would be to
use machine learning techniques to extract the relations. If we have some
labeled training data, examples of pairs of people that are in a marriage
relationship, we can train a machine learning classifier to automatically learn
the patterns for us. This sounds like a great idea, but there are several
challenges:

- How do we disambiguate between words that refer to the same entity? For
  example, a sentence may refer to "Barack Obama" as "Barack" or "The
  president".
- How do we get training data for our Machine Learning model?
- How do we deal with conflicting or uncertain data? 

### <a name="entity" href="#"></a> Entity Linking

Before starting to extract relations it is a good idea to determine which words
refer to the same "object" in the real world. These objects are called
**entities**. For example, "Barack", "Obama" or "the president" may refer to the
entity "Barack Obama". Let's say we extract relations about one of the words
above. It would be helpful to combine them as being information about the same
person.  Figuring out which words, or **mentions**, refer to the same **entity**
is a process called **entity linking**. There are various techniques to perform
entity linking, ranging from simple string matching to more sophisticated
machine learning approaches. In some domains we have a database of all known
entities to link against, such as dictionary of all countries. In other domains,
we need to be open to discovering new entities.

### Dealing with uncertainty

Given enough training data we can use machine learning algorithms to extract
entities and relations we care about. There is one problem left: human language
is inherently noisy. Words and phrases can be ambiguous, sentences are
ungrammatical, and there are spelling mistakes. Our training data may have
errors in it as well, and we may have made mistakes in the entity linking
step. This is where many machine learning approaches break down: they treat
training or input data as "correct" and make predictions using this assumption.

DeepDive makes good use of uncertainty to improve predictions during the
[probabilistic inference](inference.html) step. For example, DeepDive may figure
out that a certain mention of "Barack" is only 60% likely to actually refer to
"Barack Obama", and use this fact to discount the impact of that mention on the
final result for the entity "Barack Obama". DeepDive can also make use the
domain knowledge, and allows users to encode rules such as "If Barack is married
to Michelle, then Michelle is married to Barack" to improve the predictions.

