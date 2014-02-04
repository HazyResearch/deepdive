DROP TABLE IF EXISTS articles CASCADE;
CREATE TABLE articles(
  id bigserial primary key,
  text text
);

DROP TABLE IF EXISTS sentences CASCADE;
CREATE TABLE sentences(
  id bigserial primary key, 
  document_id bigint,
  sentence text, 
  words text[],
  pos_tags text[],
  dependencies text[],
  ner_tags text[]);


DROP TABLE IF EXISTS people_mentions CASCADE;
CREATE TABLE people_mentions(
  id bigserial primary key, 
  sentence_id bigint references sentences(id),
  start_position int,
  length int,
  text text);


DROP TABLE IF EXISTS has_spouse CASCADE;
CREATE TABLE has_spouse(
  id bigserial primary key, 
  person1_id bigint references people_mentions(id),
  person2_id bigint references people_mentions(id),
  sentence_id bigint references sentences(id),
  description text,
  is_true boolean);

DROP TABLE IF EXISTS has_spouse_features CASCADE;
CREATE TABLE has_spouse_features(
  id bigserial primary key, 
  relation_id bigint references has_spouse(id),
  feature text);