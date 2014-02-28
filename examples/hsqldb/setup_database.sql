DROP TABLE IF EXISTS hsqldb_example;
CREATE TABLE hsqldb_example(id identity, some_num double precision, some_text clob);
INSERT INTO hsqldb_example(some_num, some_text) VALUES (0, 'hello'), (1, 'world');
COMMIT;