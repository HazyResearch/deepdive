#! /usr/bin/env python
from deepdive import *

# extract training data
tagNames = ['NP', 'VP', 'PP', 'ADJP', 'ADVP', 'SBAR', 'O', 'PRT', 'CONJP', 'INTJ', 'LST', 'B', '']
sentID = 1

@tsv_extractor
@returns(lambda
        sent_id = "int",
        word_id = "int",
        word = "text",
        pos = "text",
        true_tag = "text",
        tag = "int",
        :[])

def extract(word_id="int", word="text", pos="text", ori_tag="text"):
        global sentID
        tag = ori_tag
        # get tag
        if ori_tag != '' and ori_tag is not None:
                if (tag != None and tag != 'O'):
                        tag = tag.split('-')[1]

                if tag not in tagNames:
                        tag = ''
                yield [sentID, word_id, word, pos, ori_tag, tagNames.index(tag)]
        else:
                sentID += 1
                yield [None, word_id, None, None, None, tagNames.index('')]
