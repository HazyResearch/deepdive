#!/usr/bin/env python
from deepdive import *

def tostr(v):
    return '' if v is None else str(v)

@tsv_extractor
@returns(lambda
    sent_id = 'int',
    word_id = 'int',
    feature = 'text',
    :[])
def extract(
    sent_id = 'int',
    word_id = 'int',
    word1 = 'text',
    pos1 = 'text',
    word2 = 'text',
    pos2 = 'text'
):
    yield [sent_id, word_id, 'word=%s'     % tostr(word1)                           ]
    yield [sent_id, word_id, 'pos=%s'      % tostr(pos1)                            ]
    yield [sent_id, word_id, 'prev_pos=%s' % ('' if word2 is None else tostr(pos2)) ]
