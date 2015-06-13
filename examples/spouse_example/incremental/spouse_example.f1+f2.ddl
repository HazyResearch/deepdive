articles(
  article_id text,
  text       text).

sentences(
  document_id     text,
  sentence        text,
  words           text,
  lemma           text,
  pos_tags        text,
  dependencies    text,
  ner_tags        text,
  sentence_offset int,
  sentence_id     text).

people_mentions(
  sentence_id    text,
  start_position int,
  length         int,
  text           text,
  mention_id     text).

has_spouse_candidates(
  person1_id  text,
  person2_id  text,
  sentence_id text,
  description text,
  relation_id text,
  is_true boolean).

has_spouse_features(
  relation_id text,
  feature     text).

has_spouse?(relation_id text).

people_mentions :-
  !ext_people(ext_people_input).

ext_people_input(s, words, ner_tags) :-
  sentences(a, b, words, c, d, e, ner_tags, f, s).

function ext_people over like ext_people_input
                 returns like people_mentions
  implementation "udf/ext_people.py" handles tsv lines.

has_spouse_candidates :-
  !ext_has_spouse(ext_has_spouse_input).

ext_has_spouse_input(s, p1_id, p1_text, p2_id, p2_text) :-
  people_mentions(s, a, b, p1_text, p1_id),
  people_mentions(s, c, d, p2_text, p2_id).

function ext_has_spouse over like ext_has_spouse_input
                     returns like has_spouse_candidates
  implementation "udf/ext_has_spouse.py" handles tsv lines.

has_spouse_features :-
  !ext_has_spouse_features(ext_has_spouse_features_input).

ext_has_spouse_features_input(words, rid, p1idx, p1len, p2idx, p2len) :-
  sentences(a, b, words, c, d, e, f, g, s),
  has_spouse_candidates(person1_id, person2_id, s, h, rid, x),
  people_mentions(s, p1idx, p1len, k, person1_id),
  people_mentions(s, p2idx, p2len, l, person2_id).

function ext_has_spouse_features over like ext_has_spouse_features_input
                              returns like has_spouse_features
  implementation "udf/ext_has_spouse_features.f1+f2.py" handles tsv lines.

has_spouse(rid) :- has_spouse_candidates(a, b, c, d, rid, l) label = l.

has_spouse(rid) :-
  has_spouse_candidates(a, b, c, d, rid, l),
  has_spouse_features(rid, f)
weight = f
semantics = Imply.
