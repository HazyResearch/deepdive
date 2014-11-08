DROP TABLE IF EXISTS articles CASCADE;
CREATE TABLE articles(
  article_id bigint PRIMARY KEY,
  text text
) engine=ndb;

DROP TABLE IF EXISTS sentences CASCADE;
CREATE TABLE sentences(
  document_id bigint,
  sentence text, 
  words TEXT,
  lemma TEXT,
  pos_tags TEXT,
  dependencies TEXT,
  ner_tags TEXT,
  sentence_offset bigint,
  sentence_id varchar(255) -- unique identifier for sentences
  , PRIMARY KEY (sentence_id)
  ) engine=ndb PARTITION BY KEY(sentence_id)
;


DROP TABLE IF EXISTS people_mentions CASCADE;
CREATE TABLE people_mentions(
  sentence_id varchar(255),
  start_position int,
  length int,
  text varchar(255),
  mention_id varchar(255),  -- unique identifier for people_mentions
  PRIMARY KEY(mention_id)
  ) engine=ndb
;


DROP TABLE IF EXISTS has_spouse CASCADE;
CREATE TABLE has_spouse(
  person1_id varchar(255),
  person2_id varchar(255),
  sentence_id varchar(255),
  description text,
  is_true boolean,
  relation_id varchar(255), -- unique identifier for has_spouse
  id bigint -- NOT NULL AUTO_INCREMENT,   -- reserved for DeepDive
  , PRIMARY KEY (person1_id, person2_id) -- this works for ndb
  ) engine=ndb PARTITION BY KEY (person1_id)
  ;

DROP TABLE IF EXISTS has_spouse_features CASCADE;
CREATE TABLE has_spouse_features(
  relation_id varchar(255),
  feature text
  ) engine=ndb;
