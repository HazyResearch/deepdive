INSERT INTO test(value) VALUES
  (0), (0), (0), 
  (1), (1), (1), (1), (1), (1), (1),
  (1), (1), (1), (1), (1), (1), (1),
  (2),
  (null), (null), (null), (null), (null), (null), 
  (null), (null), (null), (null), (null), (null),
  (null), (null), (null), (null), (null), (null);

ALTER SEQUENCE test2_id_seq restart with 40;
INSERT INTO test2(value) VALUES
  (0), (0), (2),
  (1), (4), (1), (1), (1), (1), (1),
  (2),
  (null), (null), (null), (null);
