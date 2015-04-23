      articles(article_id, text);
      sentences(document_id, sentence, words, lemma, pos_tags, dependencies, ner_tags, sentence_offset, sentence_id);
      people_mentions(sentence_id, start_position, length, text, mention_id);
      has_spouse(person1_id, person2_id, sentence_id, description, is_true, relation_id, id);
      has_spouse_features(relation_id, feature);
      people_mentions(sentence_id, words, ner_tags):- 
        sentences(document_id, sentence, words, lemma, pos_tags, dependencies, ner_tags, sentence_offset, sentence_id)
      udf=ext_people;
      has_spouse(sentence_id, p1.mention_id, p1.text, p2.mention_id, p2.text):-
        people_mentions(sentence_id, p1.start_position, p1.length, p1.text, p1.mention_id),
        people_mentions(sentence_id, p2.start_position, p2.length, p2.text, p2.mention_id)
      udf=ext_has_spouse;
      has_spouse_features(words, relation_id, p1.start_position, p1.length, p2.start_position, p2.length):-
        sentences(s.document_id, s.sentence, words, s.lemma, s.pos_tags, s.dependencies, s.ner_tags, s.sentence_offset, sentence_id),
        has_spouse(person1_id, person2_id, sentence_id, h.description, h.is_true, relation_id, h.id),
        people_mentions(sentence_id, p1.start_position, p1.length, p1.text, person1_id),
        people_mentions(sentence_id, p2.start_position, p2.length, p2.text, person2_id)
      udf=ext_has_spouse_features;
