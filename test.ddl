/*sentences(docid text, sentid text, wordidxs text[], words text[], poses text[], ners text[], lemmas text[], dep_paths text[], dep_parents text[], bounding_boxes text[]).
 
sentences_serialized(docid text, sentid text, wordidxs text, words text, poses text, ners text, lemmas text, dep_paths text, dep_parents text, bounding_boxes text).
 
documents(docid text, sentids text[], wordidxs text[], words text[], poses text[], ners text[], lemmas text[], dep_paths text[], dep_parents text[], bounding_boxes text[]).
 
documents_serialized(docid text, sentids text, wordidxs text, words text, poses text, ners text, lemmas text, dep_paths text, dep_parents text, bounding_boxes text).
 
entity_formation_candidate_local (docid text, type text, eid text, entity text, prov text).
 
entity_formation_candidate (docid text, type text, eid text, entity text, prov text).
 
entity_taxon_candidate_local (docid text, type text, eid text, entity text, author_year text, prov text).
 
entity_taxon_candidate (docid text, type text, eid text, entity text, author_year text, prov text).
 
entity_location_candidate (docid text, type text, eid text, entity text, prov text).
 
entity_temporal_candidate (docid text, type text, eid text, entity text, prov text).
 
entity_formation? (docid text, type text, eid text, entity text, prov text).
 
entity_taxon? (docid text, type text, eid text, entity text, author_year text, prov text).
 
entity_location? (docid text, type text, eid text, entity text, prov text).
 
entity_temporal? (docid text, type text, eid text, entity text, prov text).
 
relation_candidates (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text, features text).
 
relation_formation? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
relation_formationtemporal? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
relation_formationlocation? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
relation_taxonomy? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
relation_formation_global? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
relation_formationtemporal_global? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
relation_formationlocation_global? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
relation_taxonomy_global? (docid text, type text, eid1 text, eid2 text, entity1 text, entity2 text).
 
ddtables (docid text, tableid text, type text, sentid text).
 
interval_containments (formation text, child text, parent text).
 
interval_not_that_possible(formation text, interval1 text, interval2 text).
 
formation_per_doc(docid text, entity text[], type text[]).
 
taxon_per_doc(docid text, entity text[], type text[]).
 
document_with_formation_entities(docid text, entities text, types text, sentids text, wordidxs text, words text, poses text, ners text, lemmas text, dep_paths text, dep_parents text, bounding_boxes text).
 
document_with_taxon_entities(docid text, entities text, types text, sentids text, wordidxs text, words text, poses text, ners text, lemmas text, dep_paths text, dep_parents text, bounding_boxes text).
 
 
// Each word in sentence is separated with @@@@@
sentences_serialized(
  docid, 
  sentid, 
  array_to_string(wordidxs, "@@@@@"), 
  array_to_string(words, "@@@@@"), 
  array_to_string(poses, "@@@@@"), 
  array_to_string(ners, "@@@@@"), 
  array_to_string(lemmas, "@@@@@"), 
  array_to_string(dep_paths, "@@@@@"), 
  array_to_string(dep_parents, "@@@@@"), 
  array_to_string(bounding_boxes, "@@@@@")) *
  :- sentences(docid, sentid, wordidxs, words, poses, ners, lemmas, dep_paths, dep_parents, bounding_boxes).
 
// Intermidate table to generate `documents_serialized`.
documents(
  docid,
  ARRAY_AGG(sentid),
  ARRAY_AGG(wordidxs), 
  ARRAY_AGG(words), 
  ARRAY_AGG(poses),
  ARRAY_AGG(ners), 
  ARRAY_AGG(lemmas), 
  ARRAY_AGG(dep_paths),
  ARRAY_AGG(dep_parents), 
  ARRAY_AGG(bounding_boxes)) *
  :- sentences_serialized(docid, sentid, wordidxs, words, poses, ners, lemmas, dep_paths, dep_parents, bounding_boxes).
 
 
// Each sentence is separated with |||||, Each word in sentence is separated with @@@@@
documents_serialized(
  docid,
  array_to_string(sentids, "|||||"),
  array_to_string(wordidxs, "|||||"),
  array_to_string(words, "|||||"),
  array_to_string(poses, "|||||"),
  array_to_string(ners, "|||||"),
  array_to_string(lemmas, "|||||"),
  array_to_string(dep_paths, "|||||"),
  array_to_string(dep_parents, "|||||"),
  array_to_string(bounding_boxes, "|||||")) 
  :- documents(docid, sentids, wordidxs, words, poses, ners, lemmas, dep_paths, dep_parents, bounding_boxes).
 
/**
 * Formation that can be decided by only looking at the phrase itself.
 **/
