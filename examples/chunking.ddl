// chunking example
// https://github.com/HazyResearch/deepdive/tree/master/examples/chunking

words_raw(
  word_id bigserial,
  word text,
  pos text,
  tag text).

words(
  sent_id bigint,
  word_id bigint,
  word text,
  pos text,
  true_tag text).

word_features(
  word_id bigint,
  feature text).

tag?(word_id bigint) Categorical(13).

function ext_training 
  over like words_raw
  returns like words
  implementation "udf/ext_training.py" handles tsv lines.

words :- !ext_training(words_raw).

ext_features_input(word_id1, word1, pos1, word2, pos2) :-
  words(sent_id, word_id1, word1, pos1, tag1),
  words(sent_id, word_id2, word2, pos2, tag2).

function ext_features
  over like ext_features_input
  returns like word_features
  implementation "udf/ext_features.py" handles tsv lines.

word_features :- !ext_features(ext_features_input).

tag(word_id) :- words(word_id, a, b, c, tag) label = tag.

tag(word_id) :- word_features(word_id, f) weight = f.
