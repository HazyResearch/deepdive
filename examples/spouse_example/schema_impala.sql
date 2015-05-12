DROP TABLE IF EXISTS articles;
CREATE TABLE articles(
  article_id bigint,
  text string
) row format delimited
  fields terminated by '\t'
  stored as textfile;

DROP TABLE IF EXISTS sentences;
CREATE TABLE sentences(
  document_id bigint,
  sentence string, 
  words string,
  lemma string,
  pos_tags string,
  dependencies string,
  ner_tags string,
  sentence_offset bigint,
  sentence_id string -- unique identifier for sentences
  ) row format delimited
  fields terminated by '\t'
  stored as textfile;


DROP TABLE IF EXISTS people_mentions;
CREATE TABLE people_mentions(
  sentence_id string,
  start_position int,
  length int,
  text string,
  mention_id string  -- unique identifier for people_mentions
  );


DROP TABLE IF EXISTS has_spouse;
CREATE TABLE has_spouse(
  person1_id string,
  person2_id string,
  sentence_id string,
  description string,
  is_true boolean,
  relation_id string, -- unique identifier for has_spouse
  id bigint   -- reserved for DeepDive
  );

DROP TABLE IF EXISTS has_spouse_features;
CREATE TABLE has_spouse_features(
  relation_id string,
  feature string);
