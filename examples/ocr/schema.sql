
    CREATE TABLE features(
    id          BIGSERIAL PRIMARY KEY,
    word_id     INT,
    feature_id  INT,
    feature_val BOOLEAN);

    CREATE TABLE feature_names(
    fid     INT PRIMARY KEY,
    fname   VARCHAR(20));

    CREATE TABLE label1(
    wid   INT, val BOOLEAN,
    id    BIGINT);

    CREATE TABLE label2(
    wid   INT, val BOOLEAN,
    id    BIGINT);
