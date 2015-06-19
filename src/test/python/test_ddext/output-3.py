DROP TYPE IF EXISTS ret_func_test CASCADE;
CREATE TYPE ret_func_test AS (relation_id bigint, feature text);
CREATE OR REPLACE FUNCTION func_test(
    words text[], id bigint, p1_start int, p1_length int, p2_start int, p2_length int) RETURNS SETOF ret_func_test AS
$$

if 're' in SD:
  re = SD['re']
else:
  import re
  SD['re'] = re

if 'sys' in SD:
  sys = SD['sys']
else:
  import sys
  SD['sys'] = sys

# This function will be run for each input tuple
p1_end = p1_start + p1_length
p2_end = p2_start + p2_length

re.sub(' ', '_', ' '.join(words)) # DEBUG re library

p1_text = words[p1_start:p1_length]
p2_text = words[p2_start:p2_length]

# Features for this pair come in here
features = set()

# Feature 1: Words between the two phrases
left_idx = min(p1_end, p2_end)
right_idx = max(p1_start, p2_start)
words_between = words[left_idx:right_idx]
if words_between:
  features.add("words_between=" + "-".join(words_between))

# Feature 2: Number of words between the two phrases
features.add("num_words_between=%s" % len(words_between))

# Feature 3: Does the last word (last name) match assuming the words are not equal?
last_word_left = words[p1_end-1]
last_word_right = words[p2_end-1]
if (last_word_left == last_word_right) and (p1_text != p2_text):
  features.add("last_word_matches")

# TODO: Add more features, look at dependency paths, etc
return [[id, feature] for feature in features]

$$ LANGUAGE plpythonu ;
