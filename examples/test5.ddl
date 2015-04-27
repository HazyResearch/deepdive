      ext_people_input(
        sentence_id,
        words,
        ner_tags).
      function ext_has_spouse_features over like ext_has_spouse_features_input
                                  returns like has_spouse_features
      implementation udf/ext_has_spouse_features.py handles tsv lines.
      function ext_people over like ext_people_input
                     returns like people_mentions
      implementation udf/ext_people.py handles tsv lines.
      ext_people_input(sentence_id, words, ner_tags):- 
        sentences(document_id, sentence, words, lemma, pos_tags, dependencies, ner_tags, sentence_offset, sentence_id).
      people_mentions :-
      !ext_people(ext_people_input).
      people_mentions_1 :-
      !ext_people(people_mentions).
