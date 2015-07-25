import json
import os
import sys

DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')
NAMES_FILE = os.path.join(DATA_DIR, 'names.tsv')

dicts = {}

import string

def clean(s):
    return filter(lambda x: x in string.printable, s)

def output(record):
    sys.stdout.write(clean(json.dumps(record)) + '\n')

def log(content):
    sys.stderr.write(clean(content) + '\n')


def find_people(record):
    if 'names' not in dicts:
        dicts['names'] = set(x.strip() for x in open(NAMES_FILE).readlines())
    good_names = dicts['names']
    sid = record['sentence_id']
    words = record['words']
    ners = record['ner_tags']
    if not words:
        return
    w = words[0]
    length = 1
    if w.lower() in good_names:
        length = 99999999
        log(str(words))
    # both tuple and dict are accepted by PG
    yield [sid, 1, length, words[0] if words else '', '%s_1' % sid]


if __name__ == '__main__':
    for row in sys.stdin:
        record = json.loads(row.strip())
        for x in find_people(record):
            output(x)
