// smoke example from deepdive
// https://github.com/HazyResearch/deepdive/tree/master/examples/smoke

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
    weight = 0.5.

smoke(pid)  :- smoke(pid1), friends(pid1, pid)
    weight = 0.4.