DROP TABLE IF EXISTS articles CASCADE;
CREATE TABLE articles(
  article_id bigint,
  text text
) engine=myisam;

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
  -- sentence_id varchar(255) -- unique identifier for sentences
  sentence_id BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (sentence_id)
  ) engine=myisam
;


DROP TABLE IF EXISTS people_mentions CASCADE;
CREATE TABLE people_mentions(
  -- sentence_id varchar(255),
  sentence_id BIGINT,
  start_position int,
  length int,
  text varchar(255),
  mention_id BIGINT NOT NULL AUTO_INCREMENT,  -- unique identifier for people_mentions
  PRIMARY KEY (mention_id)
  ) engine=myisam
;


DROP TABLE IF EXISTS has_spouse CASCADE;
CREATE TABLE has_spouse(
  person1_id bigint,
  person2_id bigint,
  -- sentence_id varchar(255),
  sentence_id BIGINT,
  description text,
  is_true boolean,
  relation_id BIGINT NOT NULL AUTO_INCREMENT, -- unique identifier for has_spouse
  id bigint, -- NOT NULL AUTO_INCREMENT,   -- reserved for DeepDive
  PRIMARY KEY (relation_id)
  ) engine=myisam 
    -- PARTITION BY KEY (sentence_id)
  ; -- PARTITION BY KEY(id);

DROP TABLE IF EXISTS has_spouse_features CASCADE;
CREATE TABLE has_spouse_features(
  relation_id BIGINT,
  feature text) engine=myisam;