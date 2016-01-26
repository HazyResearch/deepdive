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
    # Rules for positive examples
    # Rule 1: Sentences that contain (<Person Candidate 1>)([ A-Za-z]+)(wife|husband)([ A-Za-z]+)(<Person Candidate 2>)
    
    cand1_last_lemma = min(p1_end, p2_end)
    cand2_first_lemma = max(p1_begin, p2_begin)
    intermediate_lemma = lemma[cand1_last_lemma+1:cand2_first_lemma]
    if ("wife" in intermediate_lemma) or ("husband" in intermediate_lemma):
	yield [p1_id, p2_id, 1]
    else:
        pass
    
    
    # Rule 2: Sentences that contain (<Person Candidate 1>)(and)?(<Person Candidate 2>)([ A-Za-z]+)(married)
    cand1_last_lemma = min(p1_end, p2_end)
    cand2_first_lemma = max(p1_begin, p2_begin)
    cand2_last_lemma = max(p1_end,p2_end)
    intermediate_lemma = tokens[cand1_last_lemma+1:cand2_first_lemma]
    tail_lemmas = tokens[cand2_last_lemma+1:]
    if ("and" in intermediate_lemma) and ("married" in tail_lemmas):
	yield [p1_id, p2_id, 1]
    else:
	pass

    # Rules for negative examples
    
    # Rule 1: Sentences that contain familial relations (<Person Candidate 1>)([ A-Za-z]+)(brother|stster|father|mother)([ A-Za-z]+)(<Person Candidate 2>)
    cand1_last_lemma = min(p1_end, p2_end)
    cand2_first_lemma = max(p1_begin, p2_begin)
    intermediate_lemma = lemma[cand1_last_lemma+1:cand2_first_lemma]
    if ("mother" in intermediate_lemma) or ("father" in intermediate_lemma) or ("sister" in intermediate_lemma) or ("brother" in intermediate_lemma):
        yield [p1_id, p2_id, -1]
    else:
        pass

