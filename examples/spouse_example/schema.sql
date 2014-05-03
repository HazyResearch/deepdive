DROP TABLE IF EXISTS articles CASCADE;
CREATE TABLE articles(
  article_id bigint,
  text text
);

DROP TABLE IF EXISTS sentences CASCADE;
CREATE TABLE sentences(
  document_id bigint,
  sentence text, 
  words text[],
  lemma text[],
  pos_tags text[],
  dependencies text[],
  ner_tags text[],
  sentence_id bigint -- unique identifier for sentences
  );


DROP TABLE IF EXISTS people_mentions CASCADE;
CREATE TABLE people_mentions(
  sentence_id bigint,
  start_position int,
  length int,
  text text,
  mention_id bigint  -- unique identifier for people_mentions
  );


DROP TABLE IF EXISTS has_spouse CASCADE;
CREATE TABLE has_spouse(
  person1_id bigint,
  person2_id bigint,
  sentence_id bigint,
  description text,
  is_true boolean,
  relation_id bigint, -- unique identifier for has_spouse
  id bigint   -- reserved for DeepDive
  );

DROP TABLE IF EXISTS has_spouse_features CASCADE;
CREATE TABLE has_spouse_features(
  relation_id bigint,
  feature text);