#!/usr/bin/env bats
load test_environ

@test "deepdive corenlp sentences-tsj usage" {
    ## first, correct usages
    # typical sentences table in DeepDive
    deepdive corenlp sentences-tsj </dev/null docid content:nlp \
                                -- >/dev/null docid nlp.{index,tokens.{word,lemma,pos,ner,characterOffsetBegin}} \
                                                    nlp.collapsed-dependencies.{dep_type,dep_token}

    # more than one NLP results per TSJ line
    deepdive corenlp sentences-tsj </dev/null docid content:nlp abstract:nlp \
                                -- >/dev/null docid nlp \
                                                    nlp.{index,tokens.{word,lemma,pos,ner,characterOffsetBegin}} \
                                                    nlp.collapsed-dependencies.{dep_type,dep_token}

    ## next, check argument parsing rules out cases that make no sense
    {

    # usage should be shown
    ! deepdive corenlp sentences-tsj
    # no columns
    ! deepdive corenlp sentences-tsj --
    # no input column
    ! deepdive corenlp sentences-tsj -- docid
    # more than one input/output separator
    ! deepdive corenlp sentences-tsj docid content:nlp -- docid nlp.index -- nlp.word
    ! deepdive corenlp sentences-tsj docid content:nlp -- -- docid nlp.index
    # no NLP result column
    ! deepdive corenlp sentences-tsj docid content \
                                  -- docid
    # more than one NLP result column
    ! deepdive corenlp sentences-tsj docid content:nlp foo:nlp2 \
                                  -- docid nlp nlp.{index,tokens.{word,pos}}

    # undefined NLP result field
    ! deepdive corenlp sentences-tsj docid content:nlp \
                                  -- docid nlp.nonexistent

    } </dev/null >/dev/null # bulletproof invocations from hanging on stdin/out
}
