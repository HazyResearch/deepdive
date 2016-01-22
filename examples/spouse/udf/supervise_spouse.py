#!/usr/bin/env python
import deepdive

@deepdive.tsv_extractor(
    input_format=[
        ( "p1_id"             , "text"   ),
        ( "p1_begin"          , "int"    ),
        ( "p1_end"            , "int"    ),
        ( "p2_id"             , "text"   ),
        ( "p2_begin"          , "int"    ),
        ( "p2_end"            , "int"    ),
        ( "doc_id"            , "text"   ),
        ( "sentence_index"    , "int"    ),
        ( "sentence_text"     , "text"   ),
        ( "tokens"            , "text[]" ),
        ( "lemmas"            , "text[]" ),
        ( "pos_tags"          , "text[]" ),
        ( "ner_tags"          , "text[]" ),
        ( "dep_types"         , "text[]" ),
        ( "dep_token_indexes" , "int[]"  ),
        ],
    output_format=[
        ( "p1_id" , "text"    ),
        ( "p2_id" , "text"    ),
        ( "label" , "boolean" ),
        ],
    )
# heuristic rules for finding positive/negative examples of spouse relationship mentions
def supervise(
        p1_id, p1_begin, p1_end,
        p2_id, p2_begin, p2_end,
        doc_id, sentence_index, sentence_text,
        tokens, lemmas, pos_tags, ner_tags, dep_types, dep_token_indexes
    ):
    # TODO ddlib generic features
    # rules for positive examples
    # TODO his wife
    # TODO her husband
    # negative examples
    # TODO ?
    pass
