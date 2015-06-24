#!/usr/bin/env bash
set -eu

deepdive sql execute "
    INSERT INTO person(person_id, name) VALUES
        (1, 'Anna'),
        (2, 'Bob'),
        (3, 'Edward'),
        (4, 'Frank'),
        (5, 'Gary'),
        (6, 'Helen')
"

deepdive sql execute "
    INSERT INTO person_smokes(person_id, smokes) VALUES
        (1, TRUE),
        (2, NULL),
        (3, TRUE),
        (4, NULL),
        (5, NULL),
        (6, NULL)
"

deepdive sql execute "
    INSERT INTO person_has_cancer(person_id, has_cancer) VALUES
        (1, NULL),
        (2, NULL),
        (3, NULL),
        (4, NULL),
        (5, NULL),
        (6, NULL)
"

deepdive sql execute "
    INSERT INTO friends(person_id, friend_id) VALUES
        (1, 2), (2, 1),
        (1, 3), (3, 1),
        (1, 4), (4, 1),
        (3, 4), (4, 3),
        (5, 6), (6, 5)
"
