import nltk
import os

tokenizer = nltk.tokenize.RegexpTokenizer(r'\w+|[^\w\s]+')

DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')
NAMES_FILE = os.path.join(DATA_DIR, 'names.tsv')

dicts = {}


def find_people(record, piggy):
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
        piggy.log(str(words))
    # if piggy.input_count > 5000:
    #     fsdflksakl
    yield (sid, 1, length, words[0] if words else '', '%s_1' % sid)
