DROP TABLE IF EXISTS person CASCADE;
DROP TABLE IF EXISTS person_has_cancer CASCADE;
DROP TABLE IF EXISTS person_smokes CASCADE;
DROP TABLE IF EXISTS friends CASCADE;

CREATE TABLE person (
    person_id bigint,
    name text
);

CREATE TABLE person_has_cancer (
    person_id bigint,
    has_cancer boolean,
    id bigint
);

CREATE TABLE person_smokes (
    person_id bigint,
    smokes boolean,
    id bigint
);

CREATE TABLE friends (
    person_id bigint,
    friend_id bigint
);
