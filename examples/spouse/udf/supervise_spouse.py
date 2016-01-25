#!/usr/bin/env python
from deepdive import *
import random

@tsv_extractor
@returns(lambda
        p1_id = "text"   ,
        p2_id = "text"   ,
        label = "int",
    :[])
# heuristic rules for finding positive/negative examples of spouse relationship mentions
def supervise(
        p1_id="text", p1_begin="int", p1_end="int",
        p2_id="text", p2_begin="int", p2_end="int",
        doc_id="text", sentence_index="int", sentence_text="text",
        tokens="text[]", lemmas="text[]", pos_tags="text[]", ner_tags="text[]",
        dep_types="text[]", dep_token_indexes="int[]",
    ):
    # TODO ddlib generic features
    # rules for positive examples
    # TODO his wife
    # TODO her husband
    # negative examples
    # TODO ?

    # TODO remove below
    x = random.random()
    if x > 0.8:
        yield [p1_id, p2_id, 1]
    elif x < 0.2:
        yield [p1_id, p2_id, -1]
    else:
        pass
