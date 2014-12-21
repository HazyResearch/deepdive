#! /usr/bin/env python

import csv, os, sys
import ddlib
from collections import defaultdict

BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
# A person can have multiple spouses
married_people = set()
spouses = set()
lines = open(BASE_DIR + '/../data/full/spouses-full.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  spouses.add((name1, name2))  # Add a non-spouse relation pair
  married_people.add(name1)
  married_people.add(name2)

# Load relations of people that are not spouse
# non_spouses = defaultdict(lambda: None)
non_spouses = set()
lines = open(BASE_DIR + '/../data/full/non-spouses-full.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair

# Load relations of people that are not spouse from error analysis
lines = open(BASE_DIR + '/non-spouses-from-errors.csv').readlines()
for line in lines:
  name1, name2 = line.strip().split(',')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair

# Load a dictionary of married words for supervision
married_words = set([l.strip() for l in 
    open(BASE_DIR + "/dicts/married.txt").readlines()])

ARRAY_DELIM = '~^~'

# For each input tuple
for row in sys.stdin:
  parts = row.strip().split('\t')
  
  sentence_id, mention_ids, mention_texts, sent_lemmas, \
      starts, lengths = parts
  
  # Unpack the inputs into arrays
  mention_ids = mention_ids.split(ARRAY_DELIM)
  mention_texts = mention_texts.split(ARRAY_DELIM)
  # Sentence lemmas for supervision
  sent_lemmas = sent_lemmas.split(ARRAY_DELIM)
  starts = [int(x) for x in starts.split(ARRAY_DELIM)]
  lengths = [int(x) for x in lengths.split(ARRAY_DELIM)]
  assert len(mention_texts) == len(mention_ids) == \
      len(starts) == len(lengths)

  num_mentions = len(mention_ids)

  # Test if there is a keyword in sentence
  kw_in_sentence = len(married_words.intersection(sent_lemmas)) > 0

  # If too many mentions occur, likely to be a list or a table, 
  # do not generate candidates for it.
  if num_mentions > 10:
    continue

  for i in range(num_mentions):
    p1_id = mention_ids[i]
    p1_text = mention_texts[i]
    p1_text_lower = p1_text.lower()

    spouse_indexes = []
    spouse_names = []
    # Scan and find if there is a known spouse in the sentence
    for j in range(num_mentions):
      if i == j: 
        continue  # do not ever extract relations with same mentions
      p2_id = mention_ids[j]
      p2_text = mention_texts[j]
      p2_text_lower = p2_text.lower()

      # DS rule 1:
      # See if the combination of people is in our supervision dictionary
      # If so, set is_correct to true or false
      if (p1_text_lower, p2_text_lower) in spouses \
          or (p2_text_lower, p1_text_lower) in spouses:
        spouse_indexes.append(j)
        spouse_names.append(p2_text_lower)

    # DS rule 2:
    # if A is sposue with B, A cannot be spouse with C.
    for j in range(num_mentions):
      if i == j: 
        continue  # do not ever extract relations with same mentions
      p2_id = mention_ids[j]
      p2_text = mention_texts[j]
      p2_text_lower = p2_text.lower()

      # See if the combination of people is in our supervision dictionary
      # If so, set is_correct to true or false
      is_true = '\N'
      supv_type = '\N'
      if j in spouse_indexes:
        is_true = '1'
        supv_type = 'SUP_KNOWN_SPOUSE'

      # elif len(spouse_indexes) > 0:
      #   # if A is sposue with B, A cannot be spouse with C.
      #   # j is not a known spouse of i, 
      #   # and j' in the sentence is a known spouse of i, 
      #   # and j' and j do not contain each other in name
      #   # so j cannot be a spouse of i
      #   if not any(name in p2_text_lower or p2_text_lower in name
      #       for name in spouse_names):
      #     is_true = '0'
      #     supv_type = 'SUP_CONFLICT'

      # Matching partial names are incorrect. Don't use for now
      # elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
      elif (p1_text == p2_text):
        is_true = '0'
        supv_type = 'SUP_SAME_PERSON'
      elif (p1_text_lower, p2_text_lower) in non_spouses:
        is_true = '0'
        supv_type = 'SUP_KNOWN_NON_SPOUSE'
      elif (p2_text_lower, p1_text_lower) in non_spouses:
        is_true = '0'
        supv_type = 'SUP_KNOWN_NON_SPOUSE'
      # elif num_mentions > 5: 
      #   # DS rule 3: Too many mentions, label all unknown as false
      #   is_true = '0'
      #   supv_type = 'SUP_TOO_MANY_MENTIONS'

      # elif not kw_in_sentence:
      #   # Create two spans of person mentions
      #   span1 = ddlib.Span(begin_word_id=starts[i], length=lengths[i])
      #   span2 = ddlib.Span(begin_word_id=starts[j], length=lengths[j])
      #   lemmas_between = ddlib.tokens_between_spans(sent_lemmas, span1, span2)

      #   # DS rule 4: if the words between two mentions are and / ,
      #   #   and there is no marriage words in sentence,
      #   #   use it as negative example.
      #   if ' '.join(lemmas_between.elements) in ['and', ',']:
      #     is_true = '0'
      #     supv_type = 'SUP_AND_NO_KW'

      # DS rule 5: if both people are married (from KB) but they are not married to each other, they are not spouse
      elif p1_text_lower in married_people and p2_text_lower in married_people:
          is_true = '0'
          supv_type = 'SUP_MARRY_OTHERS'

      print '\t'.join([
        p1_id, p2_id, sentence_id, 
        "%s-%s" %(p1_text, p2_text),
        is_true,
        supv_type,
        "%s-%s" %(p1_id, p2_id),
        '\N'   # leave "id" blank for system!
        ])

  # TABLE FORMAT: CREATE TABLE has_spouse(
  # person1_id bigint,
  # person2_id bigint,
  # sentence_id bigint,
  # description text,
  # is_true boolean,
  # relation_id bigint, -- unique identifier for has_spouse
  # id bigint   -- reserved for DeepDive
  # );
