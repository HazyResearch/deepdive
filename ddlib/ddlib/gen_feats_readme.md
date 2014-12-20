# Generic Features

This document describes the 'generic features' functionality available inside
`ddlib`. 

By 'generic features' we denote a set of features that are not application- or
domain-dependent and can be used to obtain good baseline quality for mention and
relation extractions. Feature engineering is indeed one of the most
time-consuming operation in Knowledge Base Construction and it is often
difficult to start from scratch. The aim of 'generic features' is to allow users
of DeepDive who are not KBC experts to get their application off the ground with
good starting quality.

The set of 'generic features' includes features like: the dependency path between
two mentions composing a relation mention, the Named Entity Recognition tag for
the mention, the dependency path between a mention and a keyword from a
user-specified dictionary, and so on.

The list of generic features for a mention is the following:

- The set of Part of Speech tag(s) of the word(s) composing the mention;
- The set of Named Entity Recognition tag(s) of the word(s) composing the mention;
- The set of lemmas of the word(s) composing the mention; 
- The set of word(s) composing the mention;
- The (sum of the) length(s) of the word(s) composing the mention;
- Indicator feature for whether the mention starts with a capital letter.
- The lemmas and the NER tags in a window of size up to 3 around the mention,
	both on the left and on the right of the mention. These are also combined
	(i.e., a window on the left and a window on the right are merged in a single
	feature), to give a total of (up to) 16 features (3 on left, 3 on right, 3 times 3
	combinations of left and right) for lemmas, and 16 for NERs;
- Indicator features for whether the mention (or a substring of length up to 3)
	is in user-specified dictionaries;
- Indicator features for whether keywords in user-specified dictionaries are
	present in the sentence
- The shortest dependency path(s) between the mention and keyword(s) in
	user-specified dictionary. Multiple variants of the dependency path are used
	as feature (edge labels and lemmas, edge labels only, edge labels and lemmas
	replaced with dictionary identifier if the lemma is in a dictionary);

The list of generic features for a relation is the following:
- The set of Part of Speech tags for the words between the mentions composing
	the relation;
- The set of Named Entity Recognition tags for the words between the mentions
	composing the relation;
- The set of lemmas of the words between the mentions composing the relation;
- The set of words between the mentions composing the relation;
- The sum of the lengths of the words composing the mentions;
- Indicator feature for whether the mentions start with a capital letter;
- The n-grams of size up to 3 of the lemmas and the NER tags of the words
	between the mentions composing the relation;
- The lemmas and the NERs in a window of size up to 3 around the mentions
	composing the relation. These are only combined (i.e., a left window and a
	right window are merged to give a feature), giving a total of (up to) 9
	features for the lemmas, and 9 for the NERS;
- Indicator features for whether the mentions are in user-specified
	dictionaries;
- Indicator features for whether keywords in user-specified dictionaries are
	present in the sentence
- The shortest dependency paths between the mentions and keywords in
	user-specified dictionaries. Each feature is composed by both dependency
	paths from each mention to the keyword. Multiple variants of the paths are
	used, as in the mention case;
In case the two mentions composing a relation are 'inverted' with respect to a
canonical order defined by the user, a prefix indicating this fact is prepended
to all the generic features;

In order to use the 'generic features' functionality, the user must import
`ddlib` in her Python extractor:

```
import ddlib
```

Remember to have `$DEEPDIVE_HOME/ddlib/ddlib` added to your `PYTHONPATH` in
order to be able to use `ddlib`.

The only input that the user may give is one or more dictionaries, although this
is not mandatory. Dictionaries are seen as sets of keywords that are mapped to a
dictionary identifier. All keywords in a dictionary are mapped to the same
dictionary identifier. In practice, a dictionary is a plain text file containing
one keyword per line:

```
keyword1
keyword2
keyword3
...
```

The user may load a dictionary for the 'generic
features' by using the `ddlib.load_dictionary` function:

```
import ddlib
...
ddlib.load_dictionary("marriage_keywords.txt", dict_id="marry")
...
```

The `dict_id` parameter is optional and allows the user to specify a dictionary
identifier. If it is not specified, the system will use an incremental positive
integer as dictionary identifier.
Multiple dictionaries can be loaded through multiple calls and they will all be
used during generic features generation.

To obtain the generic features for a mention, the user can use the generator
`ddlib.get_generic_features_mentions` as follows:

```
import ddlib
...
for feature in ddlib.get_generic_feature_mention(sentence, span):
	# do something with the feature
```

The first parameter `sentence` is a ordered list of `ddlib.Word` objects, where
each object represents a word in the sentence and the list is sorted according
to the order of the words in the sentence. The second parameter, `span`, is a
`ddlib.Span` object, representing the text span corresponding to the mention.

For relations, the user can obtain the generic features using the
`ddlib.get_generic_features_relations` as follows:

```
import ddlib
...
for feature in ddlib.get_generic_feature_relation(sentence, span1, span2):
	# do something with the feature
```

The parameters are respectively a ordered list of `ddlib.Word` objects and the
two `ddlib.Span` objects representing mentions composing the relation.

We remark that `ddlib.get_generic_feature_mention` and
`ddlib.get_generic_feature_relation` are [Python
generators](https://wiki.python.org/moin/Generators), so they should be used
in a loop.

