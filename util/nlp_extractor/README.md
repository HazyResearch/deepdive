# NLP Extractor

### Input

JSON of following form, where `key` and `value` are specified using command line options. For example `./run.sh --key id --value text`

    { id: [document_id], text: [raw_text] }

### Output

JSON tuple of the form:

    {
      document_id: [document_id_from_input],
      sentence: [raw_sentence_text],
      words: [array_of_words],
      post_tags: [array_of_pos_tags],
      ner_tags: [array_of_ner_tags],
      dependencies: [array of collapsed dependencies]
    }
