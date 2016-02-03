#! /usr/bin/env python
from deepdive import *

def tostr(s):
        # In TSV extractor, '\N' is NULL in psql, 'NULL' is NULL in mysql
        return '' if s is None or s in ['\N', 'NULL'] else str(s)

@tsv_extractor
@returns(lambda
	word_id = 'int',
	feature = 'text',
	:[])
def extract(word_id='int', word1='text', pos1='text', word2='text', pos2='text'):
        features = set()
        # features
        w1_word = 'word=' + tostr(word1)
        w1_pos = 'pos=' + tostr(pos1)

        # if 'w2.word' in obj.keys():
        if word2 != 'NULL' and word2 != '\N':
                # w2_word = 'prev_word=' + tostr(word2)
                w2_pos = 'prev_pos=' + tostr(pos2)
        else:
                # w2_word = 'prev_word='
                w2_pos = 'prev_pos='
        #w3_word = 'next_word=' + tostr(obj["words_raw.w3.word"])
        #w3_pos = 'next_pos=' + tostr(obj["words_raw.w3.pos"])

        features.add(w1_word)
        features.add(w1_pos)
        features.add(w2_pos)

        for f in features:
                yield [word_id, f]
