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
      sentence_offset: [0,1,2... which sentence is it in document]
      sentence_id: [document_id@sentence_offset]
    }

You can create a table like this, to be the `output_relation`:

    CREATE TABLE sentences(
      document_id bigint,
      sentence text, 
      words text[],
      lemma text[],
      pos_tags text[],
      dependencies text[],
      ner_tags text[],
      sentence_offset bigint,
      sentence_id text
      );
