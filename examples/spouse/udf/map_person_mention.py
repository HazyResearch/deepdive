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
        # generate a mention identifier
        mention_id = "%s_%d_%d_%d" % (doc_id, sentence_index, begin_index, end_index)
        mention_text = " ".join(map(lambda i: tokens[i], xrange(begin_index, end_index + 1)))
        # Output a tuple for each PERSON phrase
        yield [
            mention_id,
            mention_text,
            doc_id,
            sentence_index,
            begin_index,
            end_index
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
#        ( "mention_id"       , "text"   ),
#        ( "mention_text"     , "text"   ),
#        ( "doc_id"           , "text"   ),
#        ( "sentence_index"   , "int"    ),
#        ( "begin_index"      , "int"    ),
#        ( "end_index"        , "int"    ),
#        ],
#    generator=extract
#)

# TODO remove below once ddlib is ready
import sys
for line in sys.stdin:
    doc_id,sentence_index,tokens,ner_tags = line.split("\t")
    sentence_index = int(sentence_index)
    pgarray = lambda s: s[1:-2].split(",")
    tokens = pgarray(tokens)
    ner_tags = pgarray(ner_tags)
    for t in extract(doc_id,sentence_index,tokens,ner_tags):
        print "\t".join(map(str, t))
