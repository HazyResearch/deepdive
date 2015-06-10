person (
    person_id bigint,
    name text
).

person_has_cancer (
    person_id bigint,
    has_cancer boolean
).

person_smokes (
    person_id bigint,
    smokes boolean
).

friends (
    person_id bigint,
    friend_id bigint
).

smoke? (
    person_id bigint
).

cancer? (
    person_id bigint
).

smoke(pid)  :- person_smokes(pid, l)     label = l.
cancer(pid) :- person_has_cancer(pid, l) label = l.

cancer(pid) :- smoke(pid), person_smokes(pid, l)
    weight = 3
    label = l.

