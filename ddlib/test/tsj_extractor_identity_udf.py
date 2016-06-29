#!/usr/bin/env python
# an identity UDF for Nasty TSJ input/output
# for testing @tsj_extractor parser and formatter
from deepdive import *

def identity_for_nasty_tsj(
        v1 = "text",
        v2 = "float",
        v3 = "boolean",
        v4 = "boolean",
        v5 = "text",
        v6 = "text",
        v7 = "text",
        v8 = "text",
        v9 = "text",
        v10 = "text",
        v11 = "text",
        v12 = "int[]",
        v13 = "float[]",
        v14 = "text[]",
        v15 = "text[]",
        v16 = "text[]",
        v17 = "text[]",
        v18 = "text[]",
        v19 = "text[]",
        v20 = "text[]",
        ):
    import sys
    args = [
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            v7,
            v8,
            v9,
            v10,
            v11,
            v12,
            v13,
            v14,
            v15,
            v16,
            v17,
            v18,
            v19,
            v20,
            ]
    for i, x in enumerate(args):
        print >>sys.stderr, "v%d =\t" % (i + 1),
        print >>sys.stderr, x
    yield args
nasty_tsj_types = identity_for_nasty_tsj

@tsj_extractor
@over   (nasty_tsj_types)
@returns(nasty_tsj_types)
def extractor(**args):
    for row in identity_for_nasty_tsj(**args):
        yield row
