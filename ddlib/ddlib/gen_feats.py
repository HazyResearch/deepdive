#! /usr/bin/env python
#
# This file contains the generic features library that is included with ddlib.
#
# The three functions that a user should want to use are load_dictionary,
# get_generic_features_mention, and get_generic_features_relation.
# All the rest should be considered more or less private, except perhaps the
# get_sentence method, which is actually just a wrapper around unpack_words.
#
# Matteo, December 2014
#

from .dd import dep_path_between_words, materialize_span, Span, unpack_words

MAX_KW_LENGTH = 3

dictionaries = dict()


def load_dictionary(filename, dict_id="", func=lambda x: x):
    """Load a dictionary to be used for generic features.

    Returns the id used to identify the dictionary.

    Args:
        filename: full path to the dictionary. The dictionary is actually a set
        of words, one word per line.
        dict_id: (optional) specify the id to be used to identify the
        dictionary. By default it is a sequential number.
        func: (optional) A function to be applied to each row of the file
    """
    if dict_id == "":
        dict_id = str(len(dictionaries))
    with open(filename, 'rt') as dict_file:
        dictionary = set()
        for line in dict_file:
            dictionary.add(func(line.strip()))
        dictionary = frozenset(dictionary)
        dictionaries[str(dict_id)] = dictionary
    return str(dict_id)


def get_generic_features_mention(sentence, span, length_bin_size=5):
    """Yield 'generic' features for a mention in a sentence.

    Args:
        sentence: a list of Word objects
        span: a Span namedtuple
        length_bin_size: the size of the bins for the length feature
    """
    # Mention sequence features (words, lemmas, ners, and poses)
    for seq_feat in _get_seq_features(sentence, span):
        yield seq_feat
    # Window (left and right, up to size 3, with combinations) around the
    # mention
    for window_feat in _get_window_features(sentence, span):
        yield window_feat
    # Is (substring of) mention in a dictionary?
    for dict_indicator_feat in _get_dictionary_indicator_features(
            sentence, span):
        yield dict_indicator_feat
    # Dependency path(s) from mention to keyword(s). Various transformations of
    # the dependency path are done.
    for (i, j) in _get_substring_indices(len(sentence), MAX_KW_LENGTH):
        if i >= span.begin_word_id and i < span.begin_word_id + span.length:
            continue
        if j > span.begin_word_id and j < span.begin_word_id + span.length:
            continue
        is_in_dictionary = False
        for dict_id in dictionaries:
            if " ".join(map(lambda x: str(x.lemma), sentence[i:j])) in \
                    dictionaries[dict_id]:
                is_in_dictionary = True
                yield "KW_IND_[" + dict_id + "]"
                break
        if is_in_dictionary:
            kw_span = Span(begin_word_id=i, length=j-i)
            for dep_path_feature in _get_min_dep_path_features(
                    sentence, span, kw_span, "KW"):
                yield dep_path_feature
    # The mention starts with a capital
    if sentence[span.begin_word_id].word[0].isupper():
        yield "STARTS_WITH_CAPITAL"
    # Length of the mention
    length = len(" ".join(materialize_span(sentence, span, lambda x: x.word)))
    bin_id = length // length_bin_size
    length_feat = "LENGTH_" + str(bin_id)
    yield length_feat


def get_generic_features_relation(sentence, span1, span2, length_bin_size=5):
    """Yield 'generic' features for a relation in a sentence.

    Args:
        sentence: a list of Word objects
        span1: the first Span of the relation
        span2: the second Span of the relation
        length_bin_size: the size of the bins for the length feature
    """
    # Check whether the order of the spans is inverted. We use this information
    # to add a prefix to *all* the features.
    order = sorted([
        span1.begin_word_id, span1.begin_word_id + span1.length,
        span2.begin_word_id, span2.begin_word_id + span2.length])
    begin = order[0]
    betw_begin = order[1]
    betw_end = order[2]
    end = order[3]
    if begin == span2.begin_word_id:
        inverted = "INV_"
        yield "IS_INVERTED"
    else:
        inverted = ""
    betw_span = Span(begin_word_id=betw_begin, length=betw_end - betw_begin)
    covering_span = Span(begin_word_id=begin, length=end - begin)
    # Words, Lemmas, Ners, and Poses sequence between the mentions
    for seq_feat in _get_seq_features(sentence, betw_span):
        yield inverted + seq_feat
    # Window feature (left and right, up to size 3, combined)
    for window_feat in _get_window_features(
            sentence, covering_span, isolated=False):
        yield inverted + window_feat
    # Ngrams of up to size 3 between the mentions
    for ngram_feat in _get_ngram_features(sentence, betw_span):
        yield inverted + ngram_feat
    # Indicator features of whether the mentions are in dictionaries
    found1 = False
    for feat1 in _get_dictionary_indicator_features(
            sentence, span1, prefix=inverted + "IN_DICT"):
        found1 = True
        found2 = False
        for feat2 in _get_dictionary_indicator_features(
                sentence, span2, prefix=""):
            found2 = True
            yield feat1 + feat2
        if not found2:
            yield feat1 + "_[_NONE]"
    if not found1:
        for feat2 in _get_dictionary_indicator_features(
                sentence, span2, prefix=""):
            found2 = True
            yield inverted + "IN_DICT_[_NONE]" + feat2
    # Dependency path (and transformations) between the mention
    for betw_dep_path_feature in _get_min_dep_path_features(
            sentence, span1, span2, inverted + "BETW"):
        yield betw_dep_path_feature
    # Dependency paths (and transformations) between the mentions and keywords
    for (i, j) in _get_substring_indices(len(sentence), MAX_KW_LENGTH):
        if (i >= begin and i < betw_begin) or (i >= betw_end and i < end):
            continue
        if (j > begin and j <= betw_begin) or (j > betw_end and j <= end):
            continue
        is_in_dictionary = False
        for dict_id in dictionaries:
            if " ".join(map(lambda x: str(x.lemma), sentence[i:j])) in \
                    dictionaries[dict_id]:
                is_in_dictionary = True
                yield inverted + "KW_IND_[" + dict_id + "]"
                break
        if is_in_dictionary:
            kw_span = Span(begin_word_id=i, length=j-i)
            path1 = _get_min_dep_path(sentence, span1, kw_span)
            lemmas1 = []
            labels1 = []
            for edge in path1:
                lemmas1.append(str(edge.word2.lemma))
                labels1.append(edge.label)
            both1 = []
            for j in range(len(labels1)):
                both1.append(labels1[j])
                both1.append(lemmas1[j])
            both1 = both1[:-1]
            path2 = _get_min_dep_path(sentence, span2, kw_span)
            lemmas2 = []
            labels2 = []
            for edge in path2:
                lemmas2.append(str(edge.word2.lemma))
                labels2.append(edge.label)
            both2 = []
            for j in range(len(labels2)):
                both2.append(labels2[j])
                both2.append(lemmas2[j])
            both2 = both2[:-1]
            yield inverted + "KW_[" + " ".join(both1) + "]_[" + \
                " ".join(both2) + "]"
            yield inverted + "KW_L_[" + " ".join(labels1) + "]_[" + \
                " ".join(labels2) + "]"
            for j in range(1, len(both1), 2):
                for dict_id in dictionaries:
                    if both1[j] in dictionaries[dict_id]:
                        both1[j] = "DICT_" + str(dict_id)
                        break  # Picking up the first dictionary we find
            for j in range(1, len(both2), 2):
                for dict_id in dictionaries:
                    if both2[j] in dictionaries[dict_id]:
                        both2[j] = "DICT_" + str(dict_id)
                        break  # Picking up the first dictionary we find
            yield inverted + "KW_D_[" + " ".join(both1) + "]_[" + \
                " ".join(both2) + "]"
    # The mentions start with a capital letter
    first_capital = sentence[span1.begin_word_id].word[0].isupper()
    second_capital = sentence[span2.begin_word_id].word[0].isupper()
    capital_feat = inverted + "STARTS_WITH_CAPITAL_[" + str(first_capital) + \
        "_" + str(second_capital) + "]"
    yield capital_feat
    # The lengths of the mentions
    first_length = len(" ".join(materialize_span(
        sentence, span1, lambda x: str(x.word))))
    second_length = len(" ".join(materialize_span(
        sentence, span2, lambda x: str(x.word))))
    first_bin_id = first_length // length_bin_size
    second_bin_id = second_length // length_bin_size
    length_feat = inverted + "LENGTHS_[" + str(first_bin_id) + "_" + \
        str(second_bin_id) + "]"
    yield length_feat


def _get_substring_indices(_len, max_substring_len):
    """Yield the start-end indices for all substrings of a sequence with length
    _len, up to length max_substring_len"""
    for start in range(_len):
        for end in reversed(range(start + 1, min(
                            _len, start + 1 + max_substring_len))):
            yield (start, end)


def _get_ngram_features(sentence, span, window=3):
    """Yields ngram features. These are all substrings of size up to window in
    the part of the sentence covered by the span.

    In a typical usage, the span covers the words between two mentions, so
    this function returns all ngrams of size up to window between the two
    mentions

    Args:
        sentence: a list of Word objects
        span: the Span identifying the area for generating the substrings
        window: maximum size of a substring
    """
    for i in range(span.begin_word_id, span.begin_word_id + span.length):
        for j in range(1, window + 1):
            if i+j <= span.begin_word_id + span.length:
                yield "NGRAM_" + str(j) + "_[" + " ".join(
                    map(lambda x: str(x.lemma), sentence[i:i+j])) + "]"


def _get_min_dep_path(sentence, span1, span2):
    """Return the shortest dependency path between two Span objects

    Args:
        sentence: a list of Word objects
        span1: the first Span
        span2: the second Span
    Returns: a list of DepEdge objects
    """
    min_path = None
    min_path_length = 200  # ridiculously high number?
    for i in range(span1.begin_word_id, span1.begin_word_id + span1.length):
        for j in range(
                span2.begin_word_id, span2.begin_word_id + span2.length):
            p = dep_path_between_words(sentence, i, j)
            if len(p) < min_path_length:
                min_path = p
    return min_path


def _get_min_dep_path_features(sentence, span1, span2, prefix="BETW_"):
    """Yield the minimum dependency path features between two Span objects.
    Various variants of the dependency path are yielded:
        - using both labels and lemmas,
        - using only labels
        - using labels and lemmas, but with lemmas replaced by dict_id if the
          lemma is in a dictionary

    Args:
        sentence: a list of Word objects
        span1: the first Span
        span2: the second Span
        prefix: string prepended to all features
    """
    min_path = _get_min_dep_path(sentence, span1, span2)
    if min_path:
        min_path_lemmas = []
        min_path_labels = []
        for edge in min_path:
            min_path_lemmas.append(str(edge.word2.lemma))
            min_path_labels.append(str(edge.label))
        both = []
        for j in range(len(min_path_labels)):
            both.append(min_path_labels[j])
            both.append(min_path_lemmas[j])
        both = both[:-1]
        yield prefix + "_[" + " ".join(both) + "]"
        yield prefix + "_L_[" + " ".join(min_path_labels) + "]"
        for j in range(1, len(both), 2):
            for dict_id in dictionaries:
                if both[j] in dictionaries[dict_id]:
                    both[j] = "DICT_" + str(dict_id)
                    break  # Picking up the first dictionary we find
        yield prefix + "_D_[" + " ".join(both) + "]"


def _get_seq_features(sentence, span):
    """Yield the sequence features in a Span

    These include:
        - words sequence in the span
        - lemmas sequence in the span
        - NER tags sequence in the span
        - POS tags sequence in the span

    Args:
        sentence: a list of Word objects
        span: the Span
    """
    word_seq_feat = "WORD_SEQ_[" + " ".join(materialize_span(
        sentence, span, lambda x: x.word)) + "]"
    yield word_seq_feat
    lemma_seq_feat = "LEMMA_SEQ_[" + " ".join(materialize_span(
        sentence, span, lambda x: str(x.lemma))) + "]"
    yield lemma_seq_feat
    ner_seq_feat = "NER_SEQ_[" + " ".join(materialize_span(
        sentence, span, lambda x: str(x.ner))) + "]"
    yield ner_seq_feat
    pos_seq_feat = "POS_SEQ_[" + " ".join(materialize_span(
        sentence, span, lambda x: str(x.pos))) + "]"
    yield pos_seq_feat


def _get_window_features(
        sentence, span, window=3, combinations=True, isolated=True):
    """Yield the window features around a Span

    These are basically the n-grams around the span, up to a window of size
    'window'

    Args:
        sentence: a list of Word objects
        span: the span
        window: the maximum size of the window
        combinations: Whether to yield features that combine the windows on
            the left and on the right
        isolated: Whether to yield features that do not combine the windows on
            the left and on the right
    """
    span_end_idx = span.begin_word_id + span.length - 1
    left_lemmas = []
    left_ners = []
    right_lemmas = []
    right_ners = []
    try:
        for i in range(1, window + 1):
            lemma = str(sentence[span.begin_word_id - i].lemma)
            try:
                float(lemma)
                lemma = "_NUMBER"
            except ValueError:
                pass
            left_lemmas.append(lemma)
            left_ners.append(str(sentence[span.begin_word_id - i].ner))
    except IndexError:
        pass
    left_lemmas.reverse()
    left_ners.reverse()
    try:
        for i in range(1, window + 1):
            lemma = str(sentence[span_end_idx + i].lemma)
            try:
                float(lemma)
                lemma = "_NUMBER"
            except ValueError:
                pass
            right_lemmas.append(lemma)
            right_ners.append(str(sentence[span_end_idx + i].ner))
    except IndexError:
        pass
    if isolated:
        for i in range(len(left_lemmas)):
            yield "W_LEFT_" + str(i+1) + "_[" + " ".join(left_lemmas[-i-1:]) + \
                "]"
            yield "W_LEFT_NER_" + str(i+1) + "_[" + " ".join(left_ners[-i-1:]) +\
                "]"
        for i in range(len(right_lemmas)):
            yield "W_RIGHT_" + str(i+1) + "_[" + " ".join(right_lemmas[:i+1]) +\
                "]"
            yield "W_RIGHT_NER_" + str(i+1) + "_[" + \
                " ".join(right_ners[:i+1]) + "]"
    if combinations:
        for i in range(len(left_lemmas)):
            curr_left_lemmas = " ".join(left_lemmas[-i-1:])
            try:
                curr_left_ners = " ".join(left_ners[-i-1:])
            except TypeError:
                new_ners = []
                for ner in left_ners[-i-1:]:
                    to_add = ner
                    if not to_add:
                        to_add = "None"
                    new_ners.append(to_add)
                curr_left_ners = " ".join(new_ners)
            for j in range(len(right_lemmas)):
                curr_right_lemmas = " ".join(right_lemmas[:j+1])
                try:
                    curr_right_ners = " ".join(right_ners[:j+1])
                except TypeError:
                    new_ners = []
                    for ner in right_ners[:j+1]:
                        to_add = ner
                        if not to_add:
                            to_add = "None"
                        new_ners.append(to_add)
                    curr_right_ners = " ".join(new_ners)
                yield "W_LEMMA_L_" + str(i+1) + "_R_" + str(j+1) + "_[" + \
                    curr_left_lemmas + "]_[" + curr_right_lemmas + "]"
                yield "W_NER_L_" + str(i+1) + "_R_" + str(j+1) + "_[" + \
                    curr_left_ners + "]_[" + curr_right_ners + "]"


def _get_dictionary_indicator_features(
        sentence, span, window=3, prefix="IN_DICT"):
    """Yield the indicator features for whether a substring of the span is in
the dictionaries

    Args:
        sentence: a list of Word objects
        span: the span
        window: the maximum size of a substring
        prefix: a string to prepend to all yielded features
    """
    in_dictionaries = set()
    for i in range(window + 1):
        for j in range(span.length - i):
            phrase = " ".join(map(lambda x: str(x.lemma), sentence[j:j+i+1]))
            for dict_id in dictionaries:
                if phrase in dictionaries[dict_id]:
                    in_dictionaries.add(dict_id)
    for dict_id in in_dictionaries:
        yield prefix + "_[" + str(dict_id) + "]"
    # yield prefix + "_JOIN_[" + " ".join(
    #    map(lambda x: str(x), sorted(in_dictionaries))) + "]"


def dep_graph_parser_parenthesis(edge_str):
    """Given a string representing a dependency edge in the 'parenthesis'
    format, return a tuple of (parent_index, edge_label, child_index).

    Args:
        edge_str: a string representation of an edge in the dependency tree, in
        the format edge_label(parent_word-parent_index, child_word-child_index)
    Returns:
        tuple of (parent_index, edge_label, child_index)
    """
    tokens = edge_str.split("(")
    label = tokens[0]
    tokens = tokens[1].split(", ")
    parent = int(tokens[0].split("-")[-1]) - 1
    child = int(",".join(tokens[1:]).split("-")[-1][:-1]) - 1
    return (parent, label, child)


def dep_graph_parser_triplet(edge_str):
    """Given a string representing a dependency edge in the 'triplet' format,
    return a tuple of (parent_index, edge_label, child_index).

    Args:
        edge_str: a string representation of an edge in the dependency tree
        in the format "parent_index\tlabel\child_index"
    Returns:
        tuple of (parent_index, edge_label, child_index)
    """
    parent, label, child = edge_str.split()
    # input edge used 1-based indexing
    return (int(parent) - 1, label, int(child) - 1)


def dep_transform_parenthesis_to_triplet(edge_str):
    """Transform an edge representation from the parenthesis format to the
    triplet format"""
    parent, label, child = dep_graph_parser_parenthesis(edge_str)
    return "\t".join((str(parent + 1), label, str(child + 1)))


def dep_transform_triplet_to_parenthesis(edge_str, parent_word, child_word):
    """Transform an edge representation from the triplet format to the
    parenthesis format"""
    parent, label, child = dep_graph_parser_triplet(edge_str)
    return label + "(" + parent_word + "-" + str(parent + 1) + ", " + \
        child_word + "-" + str(child + 1) + ")"


def dep_transform_test():
    """Test the transformation functions for the various dependency paths
    formats"""

    test = "a(b-1, c-2)"
    transf = dep_transform_parenthesis_to_triplet(test)
    assert transf == "1\ta\t2"
    transf_back = dep_transform_triplet_to_parenthesis(transf, "b", "c")
    assert transf_back == test
    print("success")


def get_span(span_begin, span_length):
    """Return a Span object

    Args:
        span_begin: the index the Span begins at
        span_length: the length of the span
    """
    return Span(begin_word_id=span_begin, length=span_length)


def get_sentence(
        begin_char_offsets, end_char_offsets, words, lemmas, poses,
        dependencies, ners, dep_format_parser=dep_graph_parser_parenthesis):
    """Return a list of Word objects representing a sentence.

    This is effectively a wrapper around unpack_words, but with a less
    cumbersome interface.

    Args:
        begin_char_offsets: a list representing the beginning character offset
            for each word in the sentence
        end_char_offsets: a list representing the end character offset for each
            word in the sentence
        words: a list of the words in the sentence
        lemmas: a list of the lemmas of the words in the sentence
        poses: a list of the POS tags of the words in the sentence
        dependencies: a list of the dependency path edges for the sentence
        ners: a list of the NER tags of the words in the sentence
        dep_format_parse: a function that takes as only argument an element of
            dependencies (i.e., a dependency path edge) and returns a 3-tuple
            (parent_index, label, child_index) representing the edge. Look at
            the code for dep_graph_parser_parenthesis and
            dep_graph_parser_triplet for examples.
    """
    obj = dict()
    obj['lemma'] = lemmas
    obj['words'] = words
    obj['ner'] = ners
    obj['pos'] = poses
    obj['dep_graph'] = dependencies
    obj['ch_of_beg'] = begin_char_offsets
    obj['ch_of_end'] = end_char_offsets
    # list of Word objects
    word_obj_list = unpack_words(
        obj, character_offset_begin='ch_of_beg',
        character_offset_end='ch_of_end', lemma='lemma', pos='pos',
        ner='ner', words='words', dep_graph='dep_graph',
        dep_graph_parser=dep_format_parser)
    return word_obj_list
