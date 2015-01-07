---
layout: default
---

# Generic Feature Library

This document describes the *generic features* library that is available as part
of `ddlib`, the utility library included with DeepDive (under
`$DEEPDIVE_HOME/ddlib/ddlib`).

By 'generic features' we denote a set of features that are not application- or
domain-dependent and can be used to obtain good baseline quality for mention and
relation extractions. Feature engineering is indeed one of the most
time-consuming operation in Knowledge Base Construction and it is often
difficult to start building feature from scratch. The goal of the generic
features library is to allow users of DeepDive who are not KBC experts to get
their application off the ground with good starting quality.

The 'generic features' library leverages on Natural Language Processing (NLP)
annotations (Part of Speech, Named Entity Recognition, dependency paths, ...) to
the sentences in the corpus to build the features. Examples of features include:
the dependency path between two mentions composing a relation mention, the Named
Entity Recognition tags of the words composing a mention, the dependency path
between a mention and a keyword from a user-specified dictionary, and many
others. See below for the complete list.

The user of the library can optionally specify one or more dictionaries. These
are sets of words that the user believes are relevant for the correct
classification of mentions and relations, and are often
domain-/application-specific. The generic features library uses the dictionaries
to create additional features, allowing the inclusion of domain-knowledge in the
set of features. More details about dictionaries and their use in the library
are in the 'Using Dictionaries' section below.

## List of Generic Features

The generic feature library creates wo different sets of features for mentions
and relations, due to the different nature of these objects, and to which
features are more relevant for each type. 

There are various "classes" of generic features, which can be distinguished by
their prefix.

The list of generic features for a *mention* is the following:

- The set of Part of Speech tag(s) of the word(s) composing the mention (prefix:
	`POS_SEQ`);
- The set of Named Entity Recognition tag(s) of the word(s) composing the
	mention (`NER_SEQ`);
- The set of lemmas of the word(s) composing the mention (`LEMMA_SEQ`); 
- The set of word(s) composing the mention (`WORD_SEQ`);
- The (sum of the) length(s) of the word(s) composing the mention (`LENGTH`);
- A feature denoting whether the first word of the mention starts with a capital
	letter (`STARTS_WITH_CAPITAL`);
- The lemmas and the NER tags in a window of size up to 3 around the mention,
	both on the left and on the right of the mention. These are also combined
	(i.e., a window on the left and a window on the right are merged into a
	single feature), to give a total of (up to) 15 features (3 on left, 3 on
	right, 3 times 3 combinations of left and right) with lemmas, and 15 for
	NERs (`W`);
- Features denoting whether the mention (or a substring of it of length up to 3)
	appears in a user-specified dictionary (`IN_DICT`);
- Features indicating whether the sentence containing the mention also contains
	some keyword that appears in a user-specified dictionary (`KW_IND`);
- The shortest dependency path(s) between the mention and the keyword(s) from
	user-specified dictionaries that appear in the sentence. Multiple variants
	of the dependency path are used as feature (edge labels and lemmas, edge
	labels only, edge labels and lemmas replaced with dictionary identifier if
	the lemma is in a dictionary) (`KW`);

The list of generic features for a relation is the following (the prefixes are
the same as the ones for the mentions, except where otherwise specified):

- The set of Part of Speech tags for the words *between* the mentions in the
	relation;
- The set of Named Entity Recognition tags for the words between the mentions
	in the relation;
- The set of lemmas of the words between the mentions in the relation;
- The set of words between the mentions in the relation;
- The sum of the lengths of the words in the mentions;
- Indicator feature for whether the mentions start with a capital letter;
- The n-grams of size up to 3 of the lemmas and the NER tags of the words
	between the mentions in the relation (prefix: `NGRAM`);
- The lemmas and the NERs in a window of size up to 3 around the mentions
	composing the relation. These are only combined (i.e., a left window and a
	right window are merged into a single feature), giving a total of (up to) 9
	features for the lemmas, and 9 for the NERS;
- Features denoting whether the mentions in the relation (or substrings of them
	of size up to 3) appear in some user-specified dictionaries;
- Indicator features denoting whether the sentence containing the relation also
	contains keywords appearing in user-specified dictionaries;
- The shortest dependency paths between the mentions and keywords in
	user-specified dictionaries that are in the sentence. Each feature is
	composed by both dependency paths from each mention to the keyword. Multiple
	variants of the paths are used, as in the mention case;

If the two mentions composing a relation are 'inverted' with respect to a
canonical order defined by the user, a prefix indicating this fact is prepended
to all the generic features;

## Using the generic feature library

In order to use the 'generic features' functionality, the user must import
`ddlib` in her Python extractor:

```
import ddlib
```

`$DEEPDIVE_HOME/ddlib/ddlib` must appear in the user's `PYTHONPATH`
environmental variable in order to be able to use `ddlib`.

### Loading dictionaries

As explained in the introduction of this document, the user may optionally
specify one or more dictionaries of keywords that are used to create generic
features and can be seen as a way to incorporate domain-/application-specific
knowledge to the set of generic features. 

Dictionaries are seen as sets of keywords that are mapped to a dictionary
identifier. All keywords in a dictionary are mapped to the same dictionary
identifier. Keywords are replaced dictionary identifiers in some features, with
the effect of reducing sparsity. In practice, a dictionary is a plain text file
containing one keyword per line:

```
keyword1
keyword2
keyword3
...
```

Note that keywords can actually be composed by multiple words.

The user may load a dictionary by calling the  `ddlib.load_dictionary` function,
e.g.:

```
import ddlib
...
ddlib.load_dictionary("marriage_keywords.txt", dict_id="marry")
...
```

The `dict_id` parameter is optional and allows the user to specify the
dictionary identifier. If this is not specified, the system will use an
incremental positive integer as identifier. Multiple dictionaries can be loaded
through multiple calls and they will all be used in the generic features.

### Generating the features

The library represents features as strings.

To obtain the generic features for a mention, the library provides the generator
`ddlib.get_generic_features_mentions`, which can be used as follows:

```
import ddlib
...
for feature in ddlib.get_generic_features_mention(sentence, span):
	# do something with the feature
```

The first parameter `sentence` is a ordered list of `ddlib.Word` objects, where
each object represents a word in the sentence and the list is sorted according
to the order of the words in the sentence. The second parameter, `span`, is a
`ddlib.Span` object, representing the text span corresponding to the mention.
Consult the Pydoc documentation (and the code) for ddlib for more information
about these objects and how to generate them (especially the `get_sentence` and
`get_span` functions)

For relations, the user can obtain the generic features using the
`ddlib.get_generic_features_relations` as follows:

```
import ddlib
...
for feature in ddlib.get_generic_features_relation(sentence, span1, span2):
	# do something with the feature
```

The parameters are respectively a ordered list of `ddlib.Word` objects and the
two `ddlib.Span` objects representing mentions composing the relation.

We remark that `ddlib.get_generic_featurse_mention` and
`ddlib.get_generic_features_relation` are [Python
generators](https://wiki.python.org/moin/Generators), so they should be used
in a loop. 

Moreover, the generators may yield multiple copies of the same feature (e.g., if
a word appears twice between two mentions in a relation, the feature
`NGRAM_1_[word]` will be generated twice). It is the user's responsibility to
filter out duplicated features if needed.

