      has_spouse(person1_id, person2_id, sentence_id, description, is_true, relation_id);
      has_spouse_features(relation_id, feature);
      q(rid)!;

      q(y) :-
        has_spouse(a, b, c, d, x, y),
        has_spouse_features(y, f)
        weight = f
        label = x;
      q(y) :-
        has_spouse(a, b, c, d, x, y),
        has_spouse_features(y, f)
        weight = f
        label = x;

    //   f_has_spouse_symmetry(x, y) :-
    //     has_spouse(a1, a2, a3, a4, x, a6),
    //     has_spouse(a2, a1, b3, b4, y, b6)
    //     weight = 1;
