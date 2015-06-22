create table words_raw(
        word_id bigserial,
        word text,
        pos text,
        tag text,
        id bigint);

create table words(
        sent_id bigint,
        word_id bigint,
        word text,
        pos text,
        true_tag text,
        tag int,
        id bigint);

create table word_features(
        word_id bigint,
        feature text,
        id bigint);

