#!/usr/bin/env python

# Find phrases that are continuous words tagged with PERSON.
def extract(doc_id, sentence_index, tokens, ner_tags):
    num_tokens = len(ner_tags)
    # find all first indexes of series of tokens tagged as PERSON
    first_indexes = (i for i in xrange(num_tokens) if ner_tags[i] == "PERSON" and (i == 0 or ner_tags[i-1] != "PERSON"))
    for begin_index in first_indexes:
        # find the end of the PERSON phrase (consecutive tokens tagged as PERSON)
        end_index = begin_index + 1
        while end_index < num_tokens and ner_tags[end_index] == "PERSON":
            end_index += 1
        end_index -= 1
        mention_text = " ".join(map(lambda i: tokens[i], xrange(begin_index, end_index + 1)))
        # Output a tuple for each PERSON phrase
        yield [
            doc_id,
            sentence_index,
            begin_index,
            end_index,
            mention_text
        ]

# TODO uncomment below once ddlib is ready
#import deepdive
#deepdive.tsv_extractor(
#    input_format=[
#        ( "doc_id"           , "text"   ),
#        ( "sentence_index"   , "int"    ),
#        ( "tokens"           , "text[]" ),
#        ( "ner_tags"         , "text[]" ),
#        ],
#    output_format=[
#        ( "doc_id"           , "text"   ),
#        ( "sentence_index"   , "int"    ),
#        ( "begin_index"      , "int"    ),
#        ( "end_index"        , "int"    ),
#        ( "mention_text"     , "text"   ),
#        ],
#    generator=extract
#)

# TODO remove below once ddlib is ready
import sys
for line in sys.stdin:
    doc_id,sentence_index,tokens,ner_tags = line.split("\t")
    pgarray = lambda s: s[1:-2].split(",")
    tokens = pgarray(tokens)
    ner_tags = pgarray(ner_tags)
    for t in extract(doc_id,sentence_index,tokens,ner_tags):
        print "\t".join(map(str, t))
