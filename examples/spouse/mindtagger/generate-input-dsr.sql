-- Generate example distantly-supervised spouses for inspection of distant supervision rules
COPY (
SELECT
  s.doc_id,
  s.sentence_index,
  sl.label,
  sl.rule_id,
  s.tokens,
  pm1.mention_text,
  pm2.mention_text,
  pm1.begin_index as p1_start, 
  pm1.end_index-pm1.begin_index + 1 as p1_length, 
  pm2.begin_index as p2_start, 
  pm2.end_index-pm2.begin_index + 1 as p2_length 
FROM 
  spouse_label sl, 
  person_mention pm1, 
  person_mention pm2, 
  sentences s 
WHERE sl.p1_id = pm1.mention_id 
  AND pm1.doc_id = s.doc_id 
  AND pm1.sentence_index = s.sentence_index 
  AND sl.p2_id = pm2.mention_id 
  AND pm2.doc_id = s.doc_id 
  AND pm2.sentence_index = s.sentence_index
  AND sl.label IS NOT NULL
ORDER BY random() LIMIT 100
) TO STDOUT WITH CSV HEADER;
