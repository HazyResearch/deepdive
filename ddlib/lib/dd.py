
import sys
import collections

Word = collections.namedtuple('Word', ['begin_char_offset', 'end_char_offset', 'word', 'lemma', 'dep_par', 'dep_label'])
Span = collections.namedtuple('Span', ['begin_word_id', 'length'])
Sequence = collections.namedtuple('Sequence', ['is_inversed', 'elements'])
DepEdge = collections.namedtuple('DepEdge', ['word1', 'word2', 'label', 'is_bottom_up'])

def log(obj):
	"""Print the string form of an object to STDERR.
	
	Args:
        obj: The object that the user wants to log to STDERR.  
	"""
	sys.stderr.write(obj.__str__() + "\n")

def fe_materialize_span(words, span, func=lambda x:x):
	"""Given a sequence of objects and a span, return the subsequence that corresponds to the span.

	Args:
	    words: A sequence of objects.
	    span: A Span namedtuple
	    func: Optional function that will be applied to each element in the result subsequence.
	"""
	return map(func, words[span.begin_word_id:(span.begin_word_id+span.length)])

def _fe_seq_between_words(words, begin_idx, end_idx, func=lambda x:x):
	if begin_idx < end_idx:
		return Sequence(elements=map(func, words[begin_idx+1:end_idx]), is_inversed=False)
	else:
		return Sequence(elements=map(func, words[end_idx+1:begin_idx]), is_inversed=True)


def fe_seq_between_spans(words, span1, span2, func=lambda x:x):
	"""Given a sequence of objects and two spans, return the subsequence that is between these spans.

	Args:
	    words: A sequence of objects.
	    span1: A Span namedtuple
	    span2: A Span namedtuple
	    func: Optional function that will be applied to each element in the result subsequence.

	Returns:
	    A Sequence namedtuple between these two spans. The "is_inversed" label is set
	    to be True if span1 is *AFTER* span 2.

	"""
	if span1.begin_word_id < span2.begin_word_id:
		return _fe_seq_between_words(words, span1.begin_word_id+span1.length-1, span2.begin_word_id, func)
	else:
		return _fe_seq_between_words(words, span1.begin_word_id, span2.begin_word_id+span2.length-1, func)

def _path_to_root(words, word_idx):
	rs = []
	c_word_idx = word_idx
	while True:
		rs.append(words[c_word_idx])
		if words[c_word_idx].dep_par == -1 or words[c_word_idx].dep_par == c_word_idx:
			break
		c_word_idx = words[c_word_idx].dep_par
	return rs

def fe_dep_path_between_words(words, begin_idx, end_idx):
	"""Given a sequence of Word objects and two indices, return the sequence of Edges 
	corresponding to the dependency path between these two words.

	Args:
	    words: A sequence of Word objects.
	    span1: A word index
	    span2: A word index

	Returns:
	    An Array of Edge objects, each of which corresponds to one edge on the dependency path.
	"""
	path_to_root1 = _path_to_root(words, begin_idx)
	path_to_root2 = _path_to_root(words, end_idx)
	common = set(path_to_root1) & set(path_to_root2)
	if len(common) == 0:
		raise Exception('Dep Path Must be Wrong: No Common Element Between Word %d & %d.' % (begin_idx, end_idx))
	path = []
	for word in path_to_root1:
		if word in common: break
		path.append(DepEdge(word1=word, word2=words[word.dep_par], label=word.dep_label, is_bottom_up=True))
	path_right = []
	for word in path_to_root2:
		if word in common: break
		path_right.append(DepEdge(word1=words[word.dep_par], word2=word, label=word.dep_label, is_bottom_up=False))
	for e in reversed(path_right):
		path.append(e)
	return path


















